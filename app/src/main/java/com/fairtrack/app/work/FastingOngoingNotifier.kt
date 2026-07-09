package com.fairtrack.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fairtrack.app.MainActivity
import com.fairtrack.app.R
import com.fairtrack.app.data.FastingProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zeigt/entfernt die dauerhafte "Fasten läuft"-Notification (v0.13.0).
 *
 * KEIN Foreground Service und KEIN Prozess, der ticken müsste: Die System-UI
 * zählt den Countdown selbst über den Chronometer ([setUsesChronometer] +
 * [setWhen] auf das Fastenende). Dadurch ist der Timer auch ohne geöffnete App
 * und im Lautlos-Modus sichtbar (Channel [IMPORTANCE_LOW] = kein Ton/Heads-up).
 */
@Singleton
class FastingOngoingNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Postet die Ongoing-Notification für das laufende Fasten. Für tagebasierte
     * Protokolle (5:2) gibt es keinen Countdown, daher passiert nichts.
     */
    fun show(startMillis: Long, targetHours: Int, protocol: FastingProtocol) {
        if (protocol.isDayBased || startMillis <= 0L) return
        ensureChannel(context)

        // Ab Android 13 nur posten, wenn die Berechtigung erteilt ist.
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return

        val endMillis = startMillis + targetHours * 3_600_000L
        val now = System.currentTimeMillis()
        val goalReached = now >= endMillis

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, FastingStopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(
                context.getString(
                    R.string.fasting_ongoing_title,
                    context.getString(protocol.labelRes)
                )
            )
            .setContentText(
                context.getString(
                    if (goalReached) R.string.fasting_ongoing_reached_text
                    else R.string.fasting_ongoing_text
                )
            )
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            // Basis ist stets das Fastenende:
            //  - läuft noch  -> abwärts bis zum Ende ("Restzeit")
            //  - überschritten -> aufwärts ab dem Ende ("seit X überzogen")
            .setWhen(endMillis)
            .setChronometerCountDown(!goalReached)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, context.getString(R.string.fasting_stop), stopIntent)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    /** Entfernt die Ongoing-Notification (Fasten beendet/abgebrochen). */
    fun hide() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "fasting_ongoing"
        private const val NOTIFICATION_ID = 4713

        /** Legt den Ongoing-Channel an (idempotent, ab API 26 nötig). */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.fasting_ongoing_channel_name),
                // LOW: lautlos, kein Heads-up, aber im Silent-Modus sichtbar.
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.fasting_ongoing_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
