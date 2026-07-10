import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Signing-Material: lokal aus keystore.properties, in CI aus Umgebungsvariablen.
// Fehlt beides, bleibt der Release-Build unsigniert – so baut auch ein Checkout
// ohne Keystore (Fremdbeitrag, CI-Job ohne Secrets) noch durch.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(propertyKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propertyKey) ?: System.getenv(envKey)

val releaseStoreFile = signingValue("storeFile", "KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")

val canSignRelease = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).none { it.isNullOrBlank() }

android {
    namespace = "com.fairtrack.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fairtrack.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 16
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                // Absolute Pfade (CI-Secure-File) reicht file() unverändert durch.
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8 (Code) + Ressourcen-Shrinking. Room, Hilt, Retrofit, ML Kit und
            // kotlinx.serialization liefern ihre Keep-Regeln als Consumer-Rules mit.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        // Die CI wertet den XML-Bericht aus; HTML ist für den Menschen, der danach
        // in das Artefakt schaut.
        xmlReport = true
        htmlReport = true
        // Ein roter Lint-Lauf soll die Analyse-Stage kippen, nicht erst den
        // Release-Build über lintVitalRelease.
        warningsAsErrors = false
        abortOnError = true
        // Übersetzungen sind in values-en/-es gepflegt; fehlende Sprachen meldet
        // MissingTranslation sonst bei jedem neuen String.
        disable += "MissingTranslation"
    }

    compileOptions {
        // java.time (LocalDate) auf minSdk 24 nutzbar machen.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // Für die Versionsanzeige im "Über uns"-Bereich.
        buildConfig = true
    }
}

// Room exportiert das DB-Schema als JSON – Grundlage für korrekte Migrationen.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Per-App-Sprachumschaltung (AndroidX per-app language, v0.11.0)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Barcode-Scanner (v0.3.0): Kamera
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Barcode-Erkennung (ML Kit, gebündeltes Modell)
    implementation(libs.mlkit.barcode.scanning)

    // Open Food Facts API (Netzwerk + JSON)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Nutzerprofil-Persistenz (Preferences DataStore)
    implementation(libs.androidx.datastore.preferences)

    // Trink-Erinnerung (WorkManager v0.7.0)
    implementation(libs.androidx.work.runtime.ktx)

    // Home-Screen-Widgets (Jetpack Glance v0.10.0)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Health Connect: Import von Schritten + Aktivkalorien. Die Library verlangt
    // minSdk 26; auf 24/25 wird sie via tools:overrideLibrary geduldet und jeder
    // Aufruf hart per Build.VERSION.SDK_INT >= O geschützt (siehe HealthConnect*).
    implementation(libs.androidx.health.connect.client)

    // Produkt-Vorschaubilder (Coil, lädt OFF-Bild-URLs)
    implementation(libs.coil.compose)

    // java.time Desugaring (minSdk 24)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.ui.tooling)
}
