package com.fairtrack.app.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fairtrack.app.R
import com.fairtrack.app.data.DEFAULT_WATER_GOAL_ML
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Menge, die der Schnell-Button dem Tageswert hinzufügt. */
private const val WATER_INCREMENT_ML = 250

/**
 * Home-Screen-Widget für den Wasser-Tracker (v0.10.0). Zeigt "X / Y ml" plus
 * Fortschritt und bietet einen "+250 ml"-Button, der über [WaterAddAction]
 * ohne App-Start Wasser hinzufügt und das Widget aktualisiert. Die Daten
 * werden als Flow in der Composition beobachtet, damit jedes Widget-Update
 * (spätestens der 30-Minuten-Tick aus updatePeriodMillis) das aktuelle Datum
 * neu auswertet — sonst bleibt das Widget nach Mitternacht auf gestern stehen.
 */
class WaterWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        provideContent {
            val today = LocalDate.now().toEpochDay()
            val amount by remember(today) {
                entryPoint.waterRepository().observeForDay(today)
            }.collectAsState(initial = 0)
            val goal by remember { entryPoint.waterPreferencesRepository().goalMl }
                .collectAsState(initial = DEFAULT_WATER_GOAL_ML)

            GlanceTheme {
                WaterContent(amount, goal)
            }
        }
    }

    @Composable
    private fun WaterContent(amount: Int, goal: Int) {
        val context = LocalContext.current
        val progress = (amount.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = context.getString(R.string.widget_water_title),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = context.getString(R.string.widget_water_value, amount, goal),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                FilledButton(
                    text = context.getString(R.string.widget_water_add),
                    onClick = actionRunCallback<WaterAddAction>()
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.secondaryContainer
            )
        }
    }

    companion object {
        private val SMALL = DpSize(180.dp, 40.dp)
    }
}

/**
 * Fügt bei Klick auf den Widget-Button [WATER_INCREMENT_ML] ml zum heutigen
 * Wasserwert hinzu und aktualisiert danach alle Wasser-Widgets.
 */
class WaterAddAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters
    ) {
        // Beim Kaltstart des Prozesses löst der Launcher parallel ein Widget-Refresh
        // aus, wodurch Glance den Scope dieser Action canceln kann, bevor die
        // Room-Transaktion committed – der erste Tap ginge sonst verloren. NonCancellable
        // stellt sicher, dass Schreiben + Widget-Update immer vollständig durchlaufen.
        val appContext = context.applicationContext
        withContext(NonCancellable) {
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                WidgetEntryPoint::class.java
            )
            val today = LocalDate.now().toEpochDay()
            entryPoint.waterRepository().addWater(today, WATER_INCREMENT_ML)
            WaterWidget().updateAll(appContext)
        }
    }
}

/** AppWidget-Receiver, den der Manifest-Eintrag referenziert. */
class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}
