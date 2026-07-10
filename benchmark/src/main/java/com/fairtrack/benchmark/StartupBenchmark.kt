package com.fairtrack.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures how long FairTrack takes to draw its first frame.
 *
 * Startup is the one number every user feels on every single launch, and a
 * calorie tracker gets opened several times a day — a regression here is worse
 * than a slow screen buried three taps deep.
 *
 * Run against a real device or emulator:
 *
 *     ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * Results land in benchmark/build/outputs/connected_android_test_additional_output.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Cold start: the process does not exist yet. This is the slowest path and
     * what a user sees after a reboot or when Android has evicted the app.
     */
    @Test
    fun startupCold() = measureStartup(StartupMode.COLD)

    /**
     * Warm start: the process survives, the activity is recreated. This is the
     * common case when returning to the app after a while.
     */
    @Test
    fun startupWarm() = measureStartup(StartupMode.WARM)

    private fun measureStartup(mode: StartupMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        // Five iterations is the smallest count that still gives a usable median;
        // startup timings scatter far too much to trust a single run.
        iterations = 5,
        startupMode = mode,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        // Without waiting for real content, the metric can capture a frame that
        // only holds the splash window and reports a startup that never happened.
        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), CONTENT_TIMEOUT_MS)
    }

    private companion object {
        const val TARGET_PACKAGE = "com.fairtrack.app"
        const val CONTENT_TIMEOUT_MS = 5_000L
    }
}
