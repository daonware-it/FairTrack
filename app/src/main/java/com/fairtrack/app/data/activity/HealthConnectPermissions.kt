package com.fairtrack.app.data.activity

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord

/**
 * Health-Connect-Berechtigungen und der zugehörige Permission-Request-Contract.
 *
 * Es werden nur Lese-Rechte für Schritte und Kalorien angefragt. Aktivkalorien
 * kommen bewusst aus [ActiveCaloriesBurnedRecord] (nur Bewegung), nicht aus
 * TotalCaloriesBurnedRecord (inkl. Grundumsatz) – letzteres würde den bereits
 * im TDEE enthaltenen BMR doppelt zählen.
 *
 * Der Contract wird nur aus UI-Zweigen aufgerufen, die ausschließlich auf
 * API >= 26 mit installiertem Health Connect komponiert werden.
 */
object HealthConnectPermissions {

    val ALL: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    /**
     * Alle anzufragenden Berechtigungen inkl. Hintergrund-Lesen. Background-Read
     * ist optional (ältere Health-Connect-Versionen kennen es nicht und erteilen
     * es schlicht nicht) und darum bewusst NICHT Teil des [ALL]-Pflichtchecks —
     * ohne sie funktioniert der Sync weiterhin, nur eben nur im Vordergrund.
     */
    val REQUESTABLE: Set<String> =
        ALL + HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()
}
