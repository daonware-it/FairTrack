# FairTrack

An offline-first calorie and nutrition tracker for Android, built with Kotlin and Jetpack Compose.

All data stays on the device. No account, no cloud, no tracking.

FairTrack lets you log meals by barcode, full-text search or from your own recipes, tracks your
macros against goals derived from your body metrics, and follows your progress over time — weight,
body measurements, water intake and intermittent fasting.

> **Status:** in active development (`versionName 0.11.0`). Not yet published on the Play Store.
> Several features listed below already exist in the codebase ahead of the next version bump.

## Features

**Food logging**
- Barcode scanning via CameraX + ML Kit, resolved against the [Open Food Facts](https://openfoodfacts.org) API
- Full-text product search
- A curated offline catalogue of 116 fruits and vegetables — no network required
- Custom foods with named portions, and multi-ingredient dishes with per-serving nutrition
- Favourites, meal templates and a "recently eaten" list
- Beverages logged in millilitres, solids in grams

**Goals & body**
- Calorie and macro targets from Mifflin–St Jeor BMR × activity factor, with manual overrides
- Protein and fat scale with your goal and activity level; carbohydrates take the remainder
- BMI feedback with an age-dependent healthy range, and a guard against underweight targets
- Weight history plus body measurements (waist, body fat %, chest, arm) with trend charts

**Daily habits**
- Water tracker with a daily goal and optional interval reminders
- Intermittent fasting timer with presets (14:10, 16:8, 18:6, 20:4, OMAD, and day-based 5:2),
  a live countdown, and an ongoing notification that keeps ticking while the app is closed

**Insight & polish**
- Statistics: weekly calorie bars, a macro donut, logging streaks, micronutrient coverage
- Micronutrient tracking (14 vitamins and minerals) against EU nutrient reference values,
  with a curated offline dataset for basic foods
- Home-screen widgets (calories, compact calories, water) built with Jetpack Glance
- Light/dark themes, optional Material You, metric and imperial units
- Fully localised in German, English and Spanish

All charts are drawn directly on a Compose `Canvas` — the app pulls in no charting library.

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose (Material 3), Glance for widgets |
| Architecture | MVVM, unidirectional state via `StateFlow` |
| Dependency injection | Hilt |
| Persistence | Room (with real, versioned migrations), DataStore for preferences |
| Networking | Retrofit, OkHttp, kotlinx.serialization |
| Background work | WorkManager |
| Images | Coil |

Targets `compileSdk`/`targetSdk` 36 and `minSdk` 24 (Android 7.0), using core library desugaring so
`java.time` is available on older devices.

## Building

Requires **JDK 17** and the Android SDK.

```bash
git clone https://github.com/daonware-it/FairTrack.git
cd FairTrack
./gradlew :app:assembleDebug        # macOS / Linux
.\gradlew.bat :app:assembleDebug    # Windows
```

`local.properties` is not checked in; Android Studio will create it and point `sdk.dir` at your SDK.

Open the project in Android Studio and let it use the **Gradle wrapper** (Gradle 9.4.1) rather than a
globally installed Gradle — the Android Gradle Plugin version in use requires it.

To compile without packaging:

```bash
./gradlew :app:compileDebugKotlin
```

## Project structure

```
app/src/main/java/com/fairtrack/app/
├── data/          entities, DAOs, repositories, migrations, nutrition maths
│   ├── network/   Open Food Facts API and DTOs
│   ├── produce/   offline fruit & vegetable catalogue
│   └── bls/       offline micronutrient data for basic foods
├── di/            Hilt modules
├── ui/            one package per screen (home, search, scanner, statistics, …)
│   ├── theme/     design tokens: colours, spacing, shapes
│   └── widget/    Glance app widgets
└── work/          WorkManager workers, schedulers, notification receivers
```

### Database migrations

The Room schema is exported to `app/schemas/`, and every schema change ships an explicit `Migration`
in `data/Migrations.kt`. Destructive fallback is enabled for downgrades only, so a missing migration
fails loudly instead of silently wiping user data.

## Data sources

Nutritional data comes from [Open Food Facts](https://openfoodfacts.org), available under the
[Open Database License](https://opendatacommons.org/licenses/odbl/1-0/). Micronutrient values for
basic foods are derived from the German *Bundeslebensmittelschlüssel* (BLS 4.0), published by the
Max Rubner-Institut under CC BY 4.0.

Nutrient reference values follow the EU Food Information to Consumers Regulation (1169/2011).

## Disclaimer

FairTrack is a personal tracking tool, not a medical device. Nothing it displays is medical advice.
Nutritional values are only as accurate as their crowd-sourced and reference sources, and product
coverage — micronutrients in particular — is uneven. Consult a qualified professional before making
decisions about your diet or health.
