package com.fairtrack.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fairtrack.app.ui.navigation.FairTrackNavHost
import com.fairtrack.app.ui.theme.Dimens
import com.fairtrack.app.ui.navigation.TopLevelDestination
import com.fairtrack.app.ui.onboarding.OnboardingScreen

/**
 * Wurzel-Composable mit Onboarding-Gating beim App-Start:
 * - Status wird noch geladen (`null`) -> zentrierter Ladeindikator.
 * - Onboarding nicht abgeschlossen (`false`) -> [OnboardingScreen]. Nach dem
 *   Speichern flippt der Flow automatisch auf `true` und die App zeigt die
 *   Hauptoberfläche.
 * - Onboarding abgeschlossen (`true`) -> [MainScaffold] (Bottom-Nav + NavHost).
 */
@Composable
fun FairTrackApp() {
    val rootViewModel: RootViewModel = hiltViewModel()
    val complete by rootViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val unitSystem by rootViewModel.unitSystem.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalUnitSystem provides unitSystem) {
        when (complete) {
            null -> {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            false -> OnboardingScreen(
                onDone = {},
                modifier = Modifier.systemBarsPadding()
            )
            else -> MainScaffold()
        }
    }
}

/**
 * Hauptoberfläche: Scaffold mit Bottom Navigation und dem NavHost als Inhalt.
 */
@Composable
private fun MainScaffold() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Nur einen Eintrag pro Top-Level-Ziel im Back-Stack behalten.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon
                                else destination.unselectedIcon,
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = {
                            // NavigationBarItem misst den Label-Slot mit unbegrenzter Breite:
                            // "Einstellungen" ist als ein Wort breiter als der Item-Slot, lässt
                            // sich nicht umbrechen und liefe sonst aus dem Bildschirm. Die
                            // widthIn-Schranke gibt autoSize erst etwas zum Verkleinern.
                            Text(
                                text = stringResource(destination.labelRes),
                                modifier = Modifier.widthIn(max = Dimens.navItemLabelMaxWidth),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 8.sp,
                                    maxFontSize = MaterialTheme.typography.labelMedium.fontSize
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        FairTrackNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
