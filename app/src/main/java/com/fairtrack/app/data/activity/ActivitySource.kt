package com.fairtrack.app.data.activity

import java.time.LocalDate

/**
 * Aggregierte Bewegungsdaten eines Kalendertages – bewusst quellenagnostisch
 * (kein Health-Connect- oder Google-Fit-Typ). So kann eine zweite Quelle
 * (Google Fit) später dieselbe Schicht bedienen, ohne dass Repository, Sync
 * oder UI angefasst werden müssen.
 */
data class DailyActivity(
    val date: LocalDate,
    /** null = Wert konnte nicht gelesen werden (Upsert behält den Bestand). */
    val steps: Int?,
    val activeKcal: Int?
)

/**
 * Eine Quelle für Bewegungsdaten (Schritte + Aktivkalorien). Erste Implementierung:
 * [HealthConnectActivitySource]. Ein späterer Google-Fit-Agent liefert eine zweite
 * Implementierung und registriert sie per Hilt `@IntoSet` – die Signatur bleibt
 * dabei frei von quellenspezifischen Typen.
 */
interface ActivitySource {

    /** Stabile ID (persistiert an [com.fairtrack.app.data.entity.ActivityEntry.source]). */
    val id: String

    /** Ob die Quelle auf diesem Gerät grundsätzlich nutzbar ist (App installiert, API-Level). */
    suspend fun isAvailable(): Boolean

    /** Ob die nötigen Lese-Berechtigungen bereits erteilt wurden. */
    suspend fun hasPermissions(): Boolean

    /**
     * Liest die Tagesaggregate im (inklusiven) Datumsbereich. [sourceApps] filtert
     * auf die Paketnamen der einzahlenden Apps; ein leeres Set bedeutet ALLE
     * Quellen (Default). Fehlende Tage werden mit Nullwerten geliefert oder
     * ausgelassen; der Aufrufer upsertet idempotent.
     */
    suspend fun readDailyActivity(
        range: ClosedRange<LocalDate>,
        sourceApps: Set<String> = emptySet()
    ): List<DailyActivity>

    /**
     * Ermittelt die Paketnamen der Apps, die im Zeitraum tatsächlich Bewegungs-
     * daten geschrieben haben. Grundlage für die App-Auswahl in den Einstellungen.
     * Default leer, damit künftige Quellen ohne App-Konzept nichts liefern müssen.
     */
    suspend fun availableSourceApps(range: ClosedRange<LocalDate>): Set<String> = emptySet()
}
