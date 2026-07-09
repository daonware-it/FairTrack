package com.fairtrack.app.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fairtrack.app.MainActivity
import com.fairtrack.app.R
import com.fairtrack.app.data.NutritionGoals
import com.fairtrack.app.data.activity.activityAdjustedGoals
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Kompaktes Home-Screen-Widget: nur "Kalorien heute / Ziel" plus
 * Fortschrittsbalken, ohne Makros — bewusst ohne responsive Größenstufen,
 * damit es auch vergrößert schlicht bleibt. Ein Klick öffnet die App.
 * Datenfluss wie bei [CalorieWidget]: Flows werden in der Composition
 * beobachtet, das Datum wird bei jedem Widget-Update neu ausgewertet.
 */
class CalorieCompactWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        provideContent {
            val today = LocalDate.now().toEpochDay()
            // Budget inkl. optionalem Bewegungs-Aufschlag – identisch zum Home-Screen.
            val goals by remember(today) {
                activityAdjustedGoals(
                    entryPoint.userProfileRepository().goals,
                    entryPoint.activityRepository(),
                    entryPoint.activityPreferencesRepository(),
                    today
                )
            }.collectAsState(initial = NutritionGoals.DEFAULT)
            val calories by remember(today) {
                entryPoint.diaryRepository().observeDailyNutritionSince(today)
                    .map { days ->
                        days.firstOrNull { it.epochDay == today }?.calories?.roundToInt() ?: 0
                    }
            }.collectAsState(initial = 0)

            GlanceTheme {
                CompactContent(calories, goals)
            }
        }
    }

    @Composable
    private fun CompactContent(calories: Int, goals: NutritionGoals) {
        val context = LocalContext.current
        val progress = (calories.toFloat() / goals.calories.coerceAtLeast(1)).coerceIn(0f, 1f)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(MainActivity::class.java)),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_calories_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = context.getString(R.string.widget_calories_value, calories, goals.calories),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.secondaryContainer
            )
        }
    }
}

/** AppWidget-Receiver, den der Manifest-Eintrag referenziert. */
class CalorieCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalorieCompactWidget()
}
