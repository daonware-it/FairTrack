plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.fairtrack.benchmark"
    compileSdk = 36

    defaultConfig {
        // Macrobenchmark drives a real device or emulator through UiAutomator,
        // which needs API 24+ for the APIs used here and API 29+ for profileable.
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Macrobenchmark refuses to run on an emulator unless told to, because
        // emulator timings do not reflect a real device — a change that looks
        // like a win here can regress an actual phone.
        //
        // We suppress it anyway and accept the trade: the CI runner has no phone
        // attached, and comparing this week's median against last week's on the
        // same emulator image still catches a startup regression. Never quote
        // these numbers as "FairTrack starts in X ms" — for that, run the
        // benchmark on a device, where this argument does nothing.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Measurements only mean something against optimised code, so this module
    // builds exclusively against :app's `benchmark` type. Without the explicit
    // buildTypes block Gradle would also try to wire up debug/release variants
    // that :app cannot serve here.
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.junit)
}
