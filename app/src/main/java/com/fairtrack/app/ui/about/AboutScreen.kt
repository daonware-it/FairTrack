package com.fairtrack.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fairtrack.app.BuildConfig
import com.fairtrack.app.R
import com.fairtrack.app.ui.ExternalLinks
import com.fairtrack.app.ui.theme.Spacing

/**
 * "Über uns": App-Version, die Herkunft der Nährwertdaten und der Spendenlink.
 *
 * Die Quellenangabe ist keine Höflichkeit, sondern Lizenzpflicht: Open Food Facts
 * steht unter der Open Database License (ODbL) 1.0, der Bundeslebensmittelschlüssel
 * unter CC BY 4.0 — beide verlangen Namensnennung der Quelle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.about_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.sm)
            )

            SectionLabel(stringResource(R.string.about_section_sources))

            SourceCard(
                name = stringResource(R.string.about_off_name),
                description = stringResource(R.string.about_off_description),
                license = stringResource(R.string.about_off_license),
                url = "https://world.openfoodfacts.org/"
            )

            SourceCard(
                name = stringResource(R.string.about_bls_name),
                description = stringResource(R.string.about_bls_description),
                license = stringResource(R.string.about_bls_license),
                url = "https://www.blsdb.de/"
            )

            SectionLabel(stringResource(R.string.about_section_activity))

            SourceCard(
                name = stringResource(R.string.about_health_connect_name),
                description = stringResource(R.string.about_health_connect_description),
                license = null,
                url = null
            )

            SectionLabel(stringResource(R.string.about_section_support))

            DonationCard()

            SectionLabel(stringResource(R.string.about_section_privacy))

            SourceCard(
                name = stringResource(R.string.about_privacy_name),
                description = stringResource(R.string.about_privacy_description),
                license = null,
                url = ExternalLinks.PRIVACY_POLICY
            )

            Text(
                text = stringResource(R.string.about_disclaimer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
    }
}

/**
 * Spendenkarte.
 *
 * Der Hinweis, dass eine Spende nichts freischaltet, ist nicht bloß Bescheidenheit:
 * Google Play verlangt für jede Zahlung, die In-App-Inhalte zugänglich macht, die
 * Abwicklung über Play Billing. Freiwillige Spenden ohne Gegenleistung sind davon
 * ausgenommen — deshalb darf hier nie ein Vorteil versprochen werden.
 */
@Composable
private fun DonationCard() {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.about_donate_name),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.about_donate_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.about_donate_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xxs)
            )
            FilledTonalButton(
                onClick = { uriHandler.openUri(ExternalLinks.DONATION) },
                modifier = Modifier.padding(top = Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.about_donate_action),
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.sm)
    )
}

@Composable
private fun SourceCard(
    name: String,
    description: String,
    license: String?,
    url: String?
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (license != null) {
                Text(
                    text = license,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xxs)
                )
            }
            if (url != null) {
                TextButton(
                    onClick = { uriHandler.openUri(url) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(url)
                }
            }
        }
    }
}
