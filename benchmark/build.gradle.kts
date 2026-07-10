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
