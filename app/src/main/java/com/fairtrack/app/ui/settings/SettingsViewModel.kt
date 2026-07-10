package com.fairtrack.app.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.R
import com.fairtrack.app.data.ActivityPreferencesRepository
import com.fairtrack.app.data.AppLanguage
import com.fairtrack.app.data.AppPreferencesRepository
import com.fairtrack.app.data.DataResetRepository
import com.fairtrack.app.data.ThemeMode
import com.fairtrack.app.data.UnitSystem
import com.fairtrack.app.data.activity.ActivitySourceRegistry
import com.fairtrack.app.data.activity.ActivitySourceType
import com.fairtrack.app.data.backup.BackupRepository
import com.fairtrack.app.data.backup.ImportResult
import com.fairtrack.app.work.ActivitySyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel der Einstellungen (v0.11.0): exponiert die App-Präferenzen als
 * [StateFlow] und bietet Setter sowie das Zurücksetzen aller Nutzerdaten.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferencesRepository,
    private val activityPreferences: ActivityPreferencesRepository,
    private val activitySourceRegistry: ActivitySourceRegistry,
    private val activitySyncScheduler: ActivitySyncScheduler,
    private val dataResetRepository: DataResetRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.AUTO)

    val dynamicColor: StateFlow<Boolean> = appPreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val unitSystem: StateFlow<UnitSystem> = appPreferences.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.METRIC)

    val appLanguage: StateFlow<AppLanguage> = appPreferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    val activitySource: StateFlow<ActivitySourceType> = activityPreferences.selectedSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivitySourceType.HEALTH_CONNECT)

    val addActivityCaloriesToBudget: StateFlow<Boolean> = activityPreferences.addActivityCaloriesToBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Aktuell angehakte Quell-Apps (Paketnamen). Leeres Set = alle Apps. */
    val selectedSourceApps: StateFlow<Set<String>> = activityPreferences.selectedSourceApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Erkannte Quell-Apps (aufgelöst zu Name + Icon). null = noch nicht geladen.
    private val _availableSourceApps = MutableStateFlow<List<ActivitySourceApp>?>(null)
    val availableSourceApps: StateFlow<List<ActivitySourceApp>?> = _availableSourceApps.asStateFlow()

    init {
        refreshAvailableSourceApps()
    }

    /**
     * Ermittelt die Apps, die zuletzt Bewegungsdaten geschrieben haben (über die
     * DataOrigins der Roh-Records der letzten 30 Tage), und löst Name + Icon über
     * den [PackageManager] auf. Nicht auflösbare Pakete werden mit dem Paketnamen
     * als Fallback angezeigt.
     */
    fun refreshAvailableSourceApps() {
        viewModelScope.launch {
            val type = activityPreferences.selectedSource.first()
            val source = activitySourceRegistry.forType(type)
            if (source == null || !source.isAvailable() || !source.hasPermissions()) {
                _availableSourceApps.value = emptyList()
                return@launch
            }
            val end = LocalDate.now()
            val start = end.minusDays(DISCOVERY_WINDOW_DAYS)
            val packages = source.availableSourceApps(start..end)
            _availableSourceApps.value = packages
                .map { resolveApp(it) }
                .sortedBy { it.label.lowercase() }
        }
    }

    private fun resolveApp(packageName: String): ActivitySourceApp {
        val pm = context.packageManager
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            ActivitySourceApp(
                packageName = packageName,
                label = pm.getApplicationLabel(info).toString(),
                icon = runCatching { pm.getApplicationIcon(packageName).toBitmap().asImageBitmap() }
                    .getOrNull()
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Nicht (mehr) installiert: Paketname als Fallback.
            ActivitySourceApp(packageName = packageName, label = packageName, icon = null)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColor(enabled) }
    }

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { appPreferences.setUnitSystem(system) }
    }

    /**
     * Persistiert die Sprach-Präferenz. Die tatsächliche Locale-Umschaltung via
     * `AppCompatDelegate.setApplicationLocales(...)` erfolgt in der UI-Schicht
     * ([SettingsScreen]) auf dem Main-Thread.
     */
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { appPreferences.setAppLanguage(language) }
    }

    fun setActivitySource(source: ActivitySourceType) {
        viewModelScope.launch { activityPreferences.setSelectedSource(source) }
    }

    fun setAddActivityCaloriesToBudget(enabled: Boolean) {
        viewModelScope.launch { activityPreferences.setAddActivityCaloriesToBudget(enabled) }
    }

    /**
     * Hakt eine Quell-App an/ab. Semantik "leer = alle" bleibt kanonisch erhalten:
     * Sind am Ende alle erkannten Apps ausgewählt, wird das leere Set persistiert.
     * Nach der Änderung wird ein Re-Sync ausgelöst, damit die Card nicht die alten
     * (anders gefilterten) Werte zeigt.
     */
    fun toggleSourceApp(packageName: String) {
        viewModelScope.launch {
            val all = _availableSourceApps.value.orEmpty().map { it.packageName }.toSet()
            // Leeres persistiertes Set bedeutet "alle" -> zum Umschalten expandieren.
            val current = activityPreferences.selectedSourceApps.first().ifEmpty { all }
            val next = if (packageName in current) current - packageName else current + packageName
            val canonical = if (next.isNotEmpty() && next == all) emptySet() else next
            activityPreferences.setSelectedSourceApps(canonical)
            activitySyncScheduler.syncNow()
        }
    }

    fun resetAllData() {
        viewModelScope.launch { dataResetRepository.resetAll() }
    }

    /** Vorschlag für den Dateinamen im Speichern-Dialog, z. B. `fairtrack-2026-07-10.json`. */
    fun suggestedBackupFileName(): String = "fairtrack-${LocalDate.now()}.json"

    fun exportTo(target: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Working
            _backupState.value = try {
                backupRepository.export(target, System.currentTimeMillis())
                BackupState.Message(R.string.settings_backup_export_success)
            } catch (e: Exception) {
                BackupState.Message(R.string.settings_backup_export_error)
            }
        }
    }

    fun importFrom(source: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Working
            _backupState.value = when (backupRepository.import(source)) {
                is ImportResult.Success -> BackupState.Message(R.string.settings_backup_import_success)
                is ImportResult.TooNew -> BackupState.Message(R.string.settings_backup_import_too_new)
                ImportResult.Unreadable -> BackupState.Message(R.string.settings_backup_import_unreadable)
            }
        }
    }

    fun consumeBackupState() {
        _backupState.value = BackupState.Idle
    }

    private companion object {
        const val DISCOVERY_WINDOW_DAYS = 30L
    }
}

/** Zustand von Export/Import für die UI. */
sealed interface BackupState {
    data object Idle : BackupState
    data object Working : BackupState

    /** Abgeschlossen — [messageRes] wird als Dialog gezeigt. */
    data class Message(@StringRes val messageRes: Int) : BackupState
}

/** Eine erkannte Quell-App (Health-Connect-DataOrigin), aufgelöst für die UI. */
data class ActivitySourceApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)
