package com.fairtrack.app.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.AppLanguage
import com.fairtrack.app.data.ThemeMode
import com.fairtrack.app.data.UnitSystem
import com.fairtrack.app.data.activity.ActivitySourceType
import com.fairtrack.app.data.toLocaleListCompat
import com.fairtrack.app.ui.theme.Spacing

/**
 * Einstellungen (v0.11.0): Profil-/Ziele-Einstieg plus Darstellung (Design,
 * Material You), Einheiten, Sprache und das Zurücksetzen aller Daten.
 */
@Composable
fun SettingsScreen(
    onEditGoals: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val activitySource by viewModel.activitySource.collectAsStateWithLifecycle()
    val addActivityCalories by viewModel.addActivityCaloriesToBudget.collectAsStateWithLifecycle()
    val availableSourceApps by viewModel.availableSourceApps.collectAsStateWithLifecycle()
    val selectedSourceApps by viewModel.selectedSourceApps.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showActivitySourceDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val backupBusy = backupState is BackupState.Working

    // CreateDocument legt die Datei über den System-Dateiwähler an; die App braucht
    // dafür keine Speicherberechtigung und sieht nur die eine gewählte Datei.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri -> uri?.let(viewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importFrom) }

    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        SettingCard(
            icon = Icons.Rounded.Tune,
            title = stringResource(R.string.settings_edit_goals),
            subtitle = stringResource(R.string.settings_goals_subtitle),
            onClick = onEditGoals
        )

        SectionLabel(stringResource(R.string.settings_section_appearance))

        SettingCard(
            icon = Icons.Rounded.DarkMode,
            title = stringResource(R.string.settings_theme),
            subtitle = stringResource(themeMode.labelRes),
            onClick = { showThemeDialog = true }
        )

        SettingCard(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.settings_dynamic_color),
            subtitle = stringResource(
                if (dynamicColorSupported) R.string.settings_dynamic_color_subtitle
                else R.string.settings_dynamic_color_unavailable
            ),
            enabled = dynamicColorSupported,
            trailing = {
                Switch(
                    checked = dynamicColor && dynamicColorSupported,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                    enabled = dynamicColorSupported
                )
            },
            onClick = if (dynamicColorSupported) {
                { viewModel.setDynamicColor(!dynamicColor) }
            } else null
        )

        SectionLabel(stringResource(R.string.settings_section_general))

        SettingCard(
            icon = Icons.Rounded.Straighten,
            title = stringResource(R.string.settings_units),
            subtitle = stringResource(unitSystem.labelRes),
            onClick = { showUnitsDialog = true }
        )

        SettingCard(
            icon = Icons.Rounded.Language,
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(language.labelRes),
            onClick = { showLanguageDialog = true }
        )

        SectionLabel(stringResource(R.string.settings_section_activity))

        SettingCard(
            icon = Icons.Rounded.DirectionsWalk,
            title = stringResource(R.string.settings_activity_source),
            subtitle = stringResource(activitySource.labelRes),
            onClick = { showActivitySourceDialog = true }
        )

        SettingCard(
            icon = Icons.Rounded.LocalFireDepartment,
            title = stringResource(R.string.settings_activity_budget),
            subtitle = stringResource(R.string.settings_activity_budget_subtitle),
            trailing = {
                Switch(
                    checked = addActivityCalories,
                    onCheckedChange = { viewModel.setAddActivityCaloriesToBudget(it) }
                )
            },
            onClick = { viewModel.setAddActivityCaloriesToBudget(!addActivityCalories) }
        )

        ActivitySourceAppsSection(
            apps = availableSourceApps,
            selected = selectedSourceApps,
            onToggle = viewModel::toggleSourceApp
        )

        SectionLabel(stringResource(R.string.settings_section_data))

        SettingCard(
            icon = Icons.Rounded.Upload,
            title = stringResource(R.string.settings_backup_export),
            subtitle = stringResource(R.string.settings_backup_export_subtitle),
            enabled = !backupBusy,
            showChevron = false,
            onClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) }
        )

        SettingCard(
            icon = Icons.Rounded.Download,
            title = stringResource(R.string.settings_backup_import),
            subtitle = stringResource(R.string.settings_backup_import_subtitle),
            enabled = !backupBusy,
            showChevron = false,
            onClick = { showImportDialog = true }
        )

        SettingCard(
            icon = Icons.Rounded.DeleteForever,
            title = stringResource(R.string.settings_reset),
            subtitle = stringResource(R.string.settings_reset_subtitle),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            iconCircleColor = MaterialTheme.colorScheme.error,
            iconTint = MaterialTheme.colorScheme.onError,
            titleColor = MaterialTheme.colorScheme.onErrorContainer,
            subtitleColor = MaterialTheme.colorScheme.onErrorContainer,
            showChevron = false,
            onClick = { showResetDialog = true }
        )

        SectionLabel(stringResource(R.string.settings_section_info))

        SettingCard(
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.settings_about),
            subtitle = stringResource(R.string.settings_about_subtitle),
            onClick = onOpenAbout
        )
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme_dialog_title),
            options = ThemeMode.entries,
            selected = themeMode,
            labelFor = { stringResource(it.labelRes) },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showUnitsDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_units_dialog_title),
            options = UnitSystem.entries,
            selected = unitSystem,
            labelFor = { stringResource(it.labelRes) },
            onSelect = {
                viewModel.setUnitSystem(it)
                showUnitsDialog = false
            },
            onDismiss = { showUnitsDialog = false }
        )
    }

    if (showLanguageDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_language_dialog_title),
            options = AppLanguage.entries,
            selected = language,
            labelFor = { stringResource(it.labelRes) },
            onSelect = {
                // Präferenz persistieren ...
                viewModel.setLanguage(it)
                // ... und die Locale sofort auf dem Main-Thread umschalten.
                AppCompatDelegate.setApplicationLocales(it.toLocaleListCompat())
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showActivitySourceDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_activity_source_dialog_title),
            options = ActivitySourceType.entries,
            selected = activitySource,
            labelFor = { stringResource(it.labelRes) },
            onSelect = {
                viewModel.setActivitySource(it)
                showActivitySourceDialog = false
            },
            onDismiss = { showActivitySourceDialog = false }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Der Import überschreibt den Bestand — deshalb wird gefragt, bevor der
    // Dateiwähler überhaupt aufgeht.
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_backup_import_dialog_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        // Manche Dateimanager geben JSON als octet-stream aus; beide zulassen,
                        // sonst ist die eigene Sicherung im Wähler ausgegraut.
                        importLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "application/octet-stream"))
                    }
                ) {
                    Text(stringResource(R.string.settings_backup_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    (backupState as? BackupState.Message)?.let { state ->
        AlertDialog(
            onDismissRequest = viewModel::consumeBackupState,
            text = { Text(stringResource(state.messageRes)) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeBackupState) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}

private const val BACKUP_MIME_TYPE = "application/json"

/**
 * Auswahl der einzahlenden Apps (Health-Connect-DataOrigins). Semantik der
 * Auswahl: kein Haken bzw. alle Haken = alle Quellen. Solange die Liste noch
 * lädt (null), wird nichts gezeigt; ist sie leer, erscheint ein Hinweis, dass
 * noch keine App Daten geschrieben hat.
 */
@Composable
private fun ActivitySourceAppsSection(
    apps: List<ActivitySourceApp>?,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    if (apps == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_activity_apps),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_activity_apps_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_activity_apps_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                apps.forEach { app ->
                    // Leere Auswahl = alle Apps aktiv.
                    val checked = selected.isEmpty() || app.packageName in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onToggle(app.packageName) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = { onToggle(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.xs, top = Spacing.sm)
    )
}

/** Karten-Muster der Einstellungen: Icon-Kreis + Titel/Untertitel + Trailing. */
@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    iconCircleColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconCircleColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
            when {
                trailing != null -> trailing()
                showChevron -> Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Auswahl-Dialog mit RadioButton-Liste für eine Enum-Präferenz. */
@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) }
                            )
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) }
                        )
                        Text(
                            text = labelFor(option),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.AUTO -> R.string.theme_auto
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

private val UnitSystem.labelRes: Int
    get() = when (this) {
        UnitSystem.METRIC -> R.string.unit_metric
        UnitSystem.IMPERIAL -> R.string.unit_imperial
    }

private val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.DE -> R.string.language_de
        AppLanguage.EN -> R.string.language_en
        AppLanguage.ES -> R.string.language_es
    }

private val ActivitySourceType.labelRes: Int
    get() = when (this) {
        ActivitySourceType.HEALTH_CONNECT -> R.string.activity_source_health_connect
    }
