package com.fairtrack.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Einstiegspunkt für Hilt. Die @HiltAndroidApp-Annotation erzeugt den
 * Dependency-Injection-Container für die gesamte App.
 */
@HiltAndroidApp
class FairTrackApplication : Application()
