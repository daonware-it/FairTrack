package com.fairtrack.app.data.backup

import android.content.Context
import android.net.Uri
import com.fairtrack.app.BuildConfig
import com.fairtrack.app.data.FairTrackDatabase
import com.fairtrack.app.data.UserProfileRepository
import com.fairtrack.app.data.dao.BackupDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Ergebnis eines Imports — für die Fehleranzeige in der UI. */
sealed interface ImportResult {
    data class Success(val entryCount: Int) : ImportResult

    /** Datei ist kein FairTrack-Backup oder beschädigt. */
    data object Unreadable : ImportResult

    /** Backup stammt aus einer neueren App-Version und wird nicht verstanden. */
    data class TooNew(val formatVersion: Int) : ImportResult
}

/**
 * Export und Import sämtlicher Nutzerdaten als JSON (v0.14.0).
 *
 * Der Export umfasst alle Room-Tabellen und das Nutzerprofil. Bewusst *nicht*
 * enthalten sind reine Bedienpräferenzen — Theme, Sprache, Maßsystem, Suchhistorie
 * und der Zustand eines laufenden Fastens. Sie sind in Sekunden neu gesetzt, und
 * ein laufender Fasten-Timer, der beim Import wieder aufersteht, würde
 * Benachrichtigungen für ein längst beendetes Intervall planen.
 *
 * Der Import ersetzt den gesamten Bestand (siehe [BackupDao.replaceAll]).
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FairTrackDatabase,
    private val backupDao: BackupDao,
    private val userProfileRepository: UserProfileRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Schreibt eine Sicherung nach [target] (eine vom Nutzer über den Storage
     * Access Framework gewählte Datei).
     */
    suspend fun export(target: Uri, nowEpochMillis: Long) {
        withContext(Dispatchers.IO) {
            val backup = collect(nowEpochMillis)
            context.contentResolver.openOutputStream(target)?.use { out: OutputStream ->
                out.write(json.encodeToString(FairTrackBackup.serializer(), backup).toByteArray())
            } ?: error("Konnte $target nicht zum Schreiben öffnen")
        }
    }

    /**
     * Liest [source] und ersetzt bei Erfolg den gesamten Datenbestand.
     *
     * Die Datei wird vollständig geparst und geprüft, *bevor* die erste Zeile
     * gelöscht wird. Eine kaputte Datei kostet den Nutzer damit nichts.
     */
    suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val backup = try {
            context.contentResolver.openInputStream(source)?.use { input: InputStream ->
                json.decodeFromString<FairTrackBackup>(input.readBytes().decodeToString())
            } ?: return@withContext ImportResult.Unreadable
        } catch (e: Exception) {
            // kotlinx.serialization wirft SerializationException, ContentResolver IOException,
            // enumValueOf IllegalArgumentException. Für den Nutzer ist das dieselbe Aussage.
            return@withContext ImportResult.Unreadable
        }

        if (backup.formatVersion > BACKUP_FORMAT_VERSION) {
            return@withContext ImportResult.TooNew(backup.formatVersion)
        }

        val profile = try {
            backup.profile?.toProfile()
        } catch (e: IllegalArgumentException) {
            return@withContext ImportResult.Unreadable
        }

        backupDao.replaceAll(backup)

        // Nach der Datenbank, damit ein fehlgeschlagener Insert das Profil nicht
        // vom Bestand abkoppelt. Ohne Profil im Backup bleibt das aktuelle stehen.
        profile?.let { userProfileRepository.saveProfile(it) }

        ImportResult.Success(backup.diaryEntries.size)
    }

    private suspend fun collect(nowEpochMillis: Long) = FairTrackBackup(
        formatVersion = BACKUP_FORMAT_VERSION,
        schemaVersion = schemaVersion(),
        appVersion = BuildConfig.VERSION_NAME,
        createdAtEpochMillis = nowEpochMillis,
        profile = userProfileRepository.profile.first()?.toBackup(),
        foodItems = backupDao.allFoodItems(),
        foodPortions = backupDao.allFoodPortions(),
        diaryEntries = backupDao.allDiaryEntries(),
        dishes = backupDao.allDishes(),
        dishIngredients = backupDao.allDishIngredients(),
        mealTemplates = backupDao.allMealTemplates(),
        mealTemplateItems = backupDao.allMealTemplateItems(),
        weightEntries = backupDao.allWeightEntries(),
        bodyMeasurements = backupDao.allBodyMeasurements(),
        waterEntries = backupDao.allWaterEntries(),
        fastingSessions = backupDao.allFastingSessions(),
        activityEntries = backupDao.allActivityEntries()
    )

    /**
     * Die Schema-Version der geöffneten Datenbank. Aus dem SQLite-Header gelesen,
     * nicht aus der `@Database`-Annotation: Die ist nicht `RUNTIME`-retained, ein
     * `getAnnotation` liefert dort `null`. Und eine kopierte Konstante würde beim
     * nächsten Schema-Bump stillschweigend veralten.
     */
    private fun schemaVersion(): Int = database.openHelper.readableDatabase.version
}
