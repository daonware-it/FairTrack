package com.fairtrack.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.data.ThemeMode
import com.fairtrack.app.ui.FairTrackApp
import com.fairtrack.app.ui.RootViewModel
import com.fairtrack.app.ui.theme.FairTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FairTrackRoot()
        }
    }
}

/**
 * Liest den app-weiten Theme-Zustand aus dem [RootViewModel] und legt das Theme
 * per Recomposition (ohne `recreate()`) um [FairTrackApp]. [FairTrackApp] holt
 * denselben Activity-gescopten [RootViewModel] erneut via `hiltViewModel()`.
 */
@Composable
private fun FairTrackRoot() {
    val rootViewModel: RootViewModel = hiltViewModel()
    val themeMode by rootViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by rootViewModel.dynamicColor.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    FairTrackTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        FairTrackApp()
    }
}
