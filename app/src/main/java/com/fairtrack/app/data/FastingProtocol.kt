package com.fairtrack.app.data

import androidx.annotation.StringRes
import com.fairtrack.app.R

/**
 * Gängige Protokolle für intermittierendes Fasten (v0.13.0).
 *
 * Stundenbasierte Methoden (16:8 … OMAD) definieren ein Fasten- und ein
 * Essfenster und werden über einen Live-Countdown getrackt. [FIVE_TWO] ist
 * bewusst NICHT stundenbasiert ([isDayBased] = true): 5:2 beschreibt zwei
 * kalorienreduzierte Tage pro Woche und hat daher keinen Sekunden-Countdown –
 * das Preset dient nur als erklärender Hinweis.
 *
 * Recherche-Quellen der Erklärtexte: fasting-diet-guide.com/fasting-protocols,
 * doctronic.ai (16:8 vs 5:2 vs OMAD) und timerjoy.com (Einsteiger-Guide).
 */
enum class FastingProtocol(
    val fastingHours: Int,
    val eatingHours: Int,
    @get:StringRes val labelRes: Int,
    @get:StringRes val descriptionRes: Int,
    val isDayBased: Boolean = false
) {
    SIXTEEN_EIGHT(16, 8, R.string.fasting_preset_16_8, R.string.fasting_desc_16_8),
    EIGHTEEN_SIX(18, 6, R.string.fasting_preset_18_6, R.string.fasting_desc_18_6),
    FOURTEEN_TEN(14, 10, R.string.fasting_preset_14_10, R.string.fasting_desc_14_10),
    TWENTY_FOUR(20, 4, R.string.fasting_preset_20_4, R.string.fasting_desc_20_4),
    OMAD(23, 1, R.string.fasting_preset_omad, R.string.fasting_desc_omad),
    FIVE_TWO(0, 0, R.string.fasting_preset_5_2, R.string.fasting_desc_5_2, isDayBased = true);

    companion object {
        /** Standard-Preset für neue Nutzer: das am besten untersuchte 16:8. */
        val DEFAULT = SIXTEEN_EIGHT

        /** Defensives Lesen eines persistierten [name]; Default bei null/unbekannt. */
        fun fromNameOrDefault(name: String?): FastingProtocol {
            if (name == null) return DEFAULT
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}
