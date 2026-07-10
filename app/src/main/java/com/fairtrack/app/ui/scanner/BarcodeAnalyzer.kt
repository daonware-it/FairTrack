package com.fairtrack.app.ui.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                // ML Kit meldet US-Codes als UPC-A/UPC-E, nicht als EAN-13.
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    // androidx.annotation.OptIn, nicht kotlin.OptIn: Nur das Androidx-Pendant
    // erkennt der UnsafeOptInUsageError-Lint-Check.
    @OptIn(markerClass = [ExperimentalGetImage::class])
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                codes.firstNotNullOfOrNull(::extractProductCode)?.let(onBarcode)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    companion object {
        /** GS1 Digital Link (QR mit GTIN in der URL, z. B. …/01/04012345678901). */
        private val GS1_DIGITAL_LINK = Regex("/01/(\\d{14})")

        /** Reine Zifferncodes: EAN-8, UPC-A/E, EAN-13, GTIN-14. */
        private val NUMERIC_CODE = Regex("\\d{8}|\\d{12,14}")

        /**
         * Liefert die Produktnummer (GTIN/EAN) oder null, wenn der erkannte Code
         * kein Produkt identifiziert. Marketing-QR-Codes (beliebige URLs) auf
         * Verpackungen sind für ML Kit leichter zu lesen als der EAN-Strichcode
         * daneben und würden sonst die Suche mit einer URL statt EAN starten.
         */
        fun extractProductCode(code: Barcode): String? {
            val raw = code.rawValue?.trim() ?: return null
            return when {
                raw.matches(NUMERIC_CODE) -> raw
                // GTIN-14 aus Digital-Link-QR auf EAN-13 kürzen (führende Packungs-
                // Indikator-Null entfernen), wie von Open Food Facts erwartet.
                else -> GS1_DIGITAL_LINK.find(raw)?.groupValues?.get(1)?.takeLast(13)
            }
        }
    }
}
