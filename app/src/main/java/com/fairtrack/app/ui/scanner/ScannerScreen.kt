package com.fairtrack.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.ui.theme.Spacing

/**
 * Barcode-Scanner-Screen (v0.3.0). Fragt die Kamera-Berechtigung an, zeigt die
 * Live-Vorschau mit Zielrahmen und reagiert auf die Lookup-Zustände des
 * [ScannerViewModel]. Schließt sich selbst, sobald ein Eintrag gespeichert wurde.
 */
@Composable
fun ScannerScreen(
    onClose: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.finished.collect { onClose() }
    }

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        if (!hasCamera) launcher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (!hasCamera) {
            CameraPermissionRationale(
                onGrant = { launcher.launch(Manifest.permission.CAMERA) },
                onClose = onClose
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    onBarcode = viewModel::onBarcodeScanned,
                    modifier = Modifier.fillMaxSize()
                )

                // Zentrierter Zielrahmen.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .border(
                            width = 3.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(Spacing.lg)
                        )
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.scanner_close),
                        tint = Color.White
                    )
                }

                Text(
                    text = stringResource(R.string.scanner_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.xl)
                )

                if (uiState is ScannerUiState.LookingUp) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.scanner_looking_up),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        when (val state = uiState) {
            is ScannerUiState.Confirm -> {
                val item = state.item
                ProductConfirmDialog(
                    initialName = item.name,
                    initialBrand = item.brand,
                    initialCaloriesPer100g = item.caloriesPer100g.toString(),
                    initialProteinPer100g = item.proteinPer100g.toString(),
                    initialCarbsPer100g = item.carbsPer100g.toString(),
                    initialFatPer100g = item.fatPer100g.toString(),
                    isBeverage = item.isBeverage,
                    imageUrl = item.imageUrl,
                    initialMeal = target.mealType,
                    initialMicros = item.micros,
                    onDismiss = { viewModel.resumeScanning() },
                    onConfirm = { meal, name, amt, kcal, p, c, f, unit, micros ->
                        viewModel.addToDiary(meal, name, amt, kcal, p, c, f, unit, micros)
                    }
                )
            }

            is ScannerUiState.ManualEntry -> {
                ProductConfirmDialog(
                    initialMeal = target.mealType,
                    onDismiss = { viewModel.resumeScanning() },
                    onConfirm = { meal, name, amt, kcal, p, c, f, unit, micros ->
                        viewModel.addToDiary(meal, name, amt, kcal, p, c, f, unit, micros)
                    }
                )
            }

            is ScannerUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resumeScanning() },
                    title = { Text(stringResource(R.string.scanner_title)) },
                    text = { Text(stringResource(state.messageRes)) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resumeScanning() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun CameraPermissionRationale(
    onGrant: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.scanner_close)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.scanner_permission_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.scanner_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onGrant) {
                Text(stringResource(R.string.scanner_grant_permission))
            }
        }
    }
}
