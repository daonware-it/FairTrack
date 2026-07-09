package com.fairtrack.app.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fairtrack.app.R
import com.fairtrack.app.ui.theme.FairTrackTheme
import com.fairtrack.app.ui.theme.Spacing

/**
 * Zeigt die Datenschutz-Rationale für die Health-Connect-Berechtigungen. Health
 * Connect verlinkt hierauf ("Datenschutzerklärung lesen") und ruft die Activity
 * ab Android 14 zusätzlich über den Berechtigungsnutzungs-Intent auf. Google
 * verlangt eine solche Erklärung für die Freigabe der Gesundheitsberechtigungen.
 */
class HealthPermissionsRationaleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RationaleRoot() }
    }
}

@Composable
private fun RationaleRoot() {
    FairTrackTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Text(
                    text = stringResource(R.string.activity_rationale_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.activity_rationale_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
