package com.fairtrack.app.data.activity

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord

/**
 * Health-Connect-Berechtigungen und der zugehörige Permission-Request-Contract.
 *
 * Es werden nur Lese-Rechte für Schritte und Kalorien angefragt. Aktivkalorien
 * kommen bewusst aus [ActiveCaloriesBurnedRecord] (nur Bewegung), nicht aus
 * [TotalCaloriesBurnedRecord] (inkl. Grundumsatz) – letzteres würde den bereits
 * im TDEE enthaltenen BMR doppelt zählen. Das TOTAL-Leserecht wird nur
 * angefragt, um bei Bedarf einen Fallback anbieten zu können; verwendet wird
 * ausschließlich der aktive Wert.
 *
 * Der Contract wird nur aus UI-Zweigen aufgerufen, die ausschließlich auf
 * API >= 26 mit installiertem Health Connect komponiert werden.
 */
object HealthConnectPermissions {

    val ALL: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()
}
