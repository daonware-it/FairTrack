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
import androidx.glance.LocalSize
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
import com.fairtrack.app.MainActivity
import com.fairtrack.app.R
import com.fairtrack.app.data.NutritionGoals
import com.fairtrack.app.data.activity.activityAdjustedGoals
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Home-Screen-Widget mit den heutigen Kalorien (v0.10.0). Kleines Layout zeigt
 * "Kalorien X / Ziel" plus Fortschrittsbalken, größere Layouts zusätzlich die
 * Makros in Gramm. Ein Klick öffnet die App. Die Daten werden als Flow in der
 * Composition beobachtet, damit jedes Widget-Update (spätestens der 30-Minuten-
 * Tick aus updatePeriodMillis) das aktuelle Datum neu auswertet — sonst bleibt
 * das Widget nach Mitternacht auf den gestrigen Werten stehen.
 */
class CalorieWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

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
            val nutrition by remember(today) {
                entryPoint.diaryRepository().observeDailyNutritionSince(today)
                    .map { days -> days.firstOrNull { it.epochDay == today } }
            }.collectAsState(initial = null)

            GlanceTheme {
                CalorieContent(
                    calories = nutrition?.calories?.roundToInt() ?: 0,
                    goals = goals,
                    protein = nutrition?.protein?.roundToInt() ?: 0,
                    carbs = nutrition?.carbs?.roundToInt() ?: 0,
                    fat = nutrition?.fat?.roundToInt() ?: 0
                )
            }
        }
    }

    @Composable
    private fun CalorieContent(
        calories: Int,
        goals: NutritionGoals,
        protein: Int,
        carbs: Int,
        fat: Int
    ) {
        val context = LocalContext.current
        val size = LocalSize.current
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
            if (size.height >= MEDIUM.height) {
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    MacroLabel(context.getString(R.string.widget_macro_protein, protein))
                    Spacer(GlanceModifier.width(10.dp))
                    MacroLabel(context.getString(R.string.widget_macro_carbs, carbs))
                    Spacer(GlanceModifier.width(10.dp))
                    MacroLabel(context.getString(R.string.widget_macro_fat, fat))
                }
            }
        }
    }

    @Composable
    private fun MacroLabel(text: String) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }

    companion object {
        private val SMALL = DpSize(110.dp, 40.dp)
        private val MEDIUM = DpSize(180.dp, 110.dp)
    }
}

/** AppWidget-Receiver, den der Manifest-Eintrag referenziert. */
class CalorieWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalorieWidget()
}
