package com.fairtrack.app.data.activity

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * [ActivitySource] auf Basis von Health Connect (Schritte + Aktivkalorien).
 *
 * Die connect-client-Library verlangt minSdk 26 – die App läuft aber ab 24.
 * Deshalb ist JEDE Health-Connect-Berührung hart per
 * `Build.VERSION.SDK_INT >= O` abgesichert; auf 24/25 meldet die Quelle sich als
 * nicht verfügbar und die Home-Card blendet sich aus. Der [HealthConnectClient]
 * wird bewusst pro Aufruf erzeugt (kein Feld), damit das Laden der Klasse auf
 * älteren Geräten nichts anstößt.
 */
class HealthConnectActivitySource(
    private val context: Context
) : ActivitySource {

    override val id: String = ActivitySourceType.HEALTH_CONNECT.sourceId

    override suspend fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    override suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(HealthConnectPermissions.ALL)
    }

    override suspend fun readDailyActivity(
        range: ClosedRange<LocalDate>,
        sourceApps: Set<String>
    ): List<DailyActivity> {
        if (!isAvailable() || !hasPermissions()) return emptyList()
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault()
        // Leeres Set = ALLE Quellen (kein Filter). Sonst nur die gewählten Apps.
        val originFilter: Set<DataOrigin> =
            if (sourceApps.isEmpty()) emptySet() else sourceApps.map { DataOrigin(it) }.toSet()

        val result = mutableListOf<DailyActivity>()
        var day = range.start
        while (day <= range.endInclusive) {
            val startInstant = day.atStartOfDay(zone).toInstant()
            val endInstant = day.plusDays(1).atStartOfDay(zone).toInstant()
            val filter = TimeRangeFilter.between(startInstant, endInstant)

            // Fehlgeschlagene Reads (z. B. SecurityException im Hintergrund, Rate
            // Limit) liefern null statt 0 — sonst würde der Upsert im Worker
            // bereits importierte echte Werte mit 0 überschreiben.
            val steps = runCatching {
                val response = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = filter,
                        dataOriginFilter = originFilter
                    )
                )
                (response[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
            }.getOrNull()

            val activeKcal = runCatching {
                val response = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = filter,
                        dataOriginFilter = originFilter
                    )
                )
                response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    ?.inKilocalories?.roundToInt() ?: 0
            }.getOrNull()

            if (steps != null || activeKcal != null) {
                result += DailyActivity(date = day, steps = steps, activeKcal = activeKcal)
            }
            day = day.plusDays(1)
        }
        return result
    }

    override suspend fun availableSourceApps(range: ClosedRange<LocalDate>): Set<String> {
        if (!isAvailable() || !hasPermissions()) return emptySet()
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault()
        val filter = TimeRangeFilter.between(
            range.start.atStartOfDay(zone).toInstant(),
            range.endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
        )

        // Es gibt keine "liste verbundene Apps"-API. Wir leiten die einzahlenden
        // Apps aus den DataOrigins der zuletzt geschriebenen Roh-Records ab
        // (Schritte + Aktivkalorien).
        val packages = mutableSetOf<String>()
        runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = filter
                )
            ).records.forEach { packages += it.metadata.dataOrigin.packageName }
        }
        runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = filter
                )
            ).records.forEach { packages += it.metadata.dataOrigin.packageName }
        }
        // Leere Paketnamen defensiv aussortieren.
        return packages.filter { it.isNotBlank() }.toSet()
    }
}
