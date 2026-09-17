package com.freshveg.app.automation

import com.freshveg.app.core.utils.MandiTranslationUtils
import com.freshveg.app.core.utils.formatSafeDate
import com.freshveg.app.core.utils.safeDate
import org.junit.Assert.*
import org.junit.Test

class UtilsAndFormattersAutomationTest {

    @Test
    fun testSafeDateCrashProofing() {
        // Standard ISO Timestamp
        val isoDate = "2026-08-25T11:54:30.000Z"
        assertEquals("2026-08-25", isoDate.safeDate())

        // Substring length < 10 (previously crashed with IndexOutOfBoundsException)
        val shortDate = "2026-08"
        assertEquals("2026-08", shortDate.safeDate())

        // Single character string
        val singleChar = "2"
        assertEquals("2", singleChar.safeDate())

        // Null string
        val nullDate: String? = null
        assertEquals("N/A", nullDate.safeDate())

        // Empty string
        val emptyDate = ""
        assertEquals("N/A", emptyDate.safeDate())

        // Whitespace string
        val blankDate = "   "
        assertEquals("N/A", blankDate.safeDate())

        // Custom fallback
        assertEquals("No Date", nullDate.safeDate(fallback = "No Date"))
    }

    @Test
    fun testFormatSafeDateDualFallbacks() {
        // Both primary and fallback present -> takes primary
        assertEquals("2026-08-25", formatSafeDate("2026-08-25T00:00:00Z", "2026-08-20T00:00:00Z"))

        // Primary null, secondary present -> takes secondary
        assertEquals("2026-08-20", formatSafeDate(null, "2026-08-20T00:00:00Z"))

        // Primary blank, secondary present -> takes secondary
        assertEquals("2026-08-20", formatSafeDate("   ", "2026-08-20T00:00:00Z"))

        // Both null -> returns default fallback
        assertEquals("N/A", formatSafeDate(null, null))
    }

    @Test
    fun testMandiTranslationExactAndHindiLookups() {
        assertEquals("आलू", MandiTranslationUtils.translateEnglishToHindi("Potato"))
        assertEquals("टमाटर", MandiTranslationUtils.translateEnglishToHindi("Tomato"))
        assertEquals("प्याज", MandiTranslationUtils.translateEnglishToHindi("Onion"))
        assertEquals("लहसुन", MandiTranslationUtils.translateEnglishToHindi("Garlic"))
        assertEquals("अदरक", MandiTranslationUtils.translateEnglishToHindi("Ginger"))
        assertEquals("भिंडी", MandiTranslationUtils.translateEnglishToHindi("Okra / Lady Finger"))

        // Reverse lookup Hindi to English
        assertEquals("Potato", MandiTranslationUtils.translateHindiToEnglish("आलू"))
        assertEquals("Tomato", MandiTranslationUtils.translateHindiToEnglish("टमाटर"))
    }

    @Test
    fun testMandiTranslationAliasesAndCaseInsensitivity() {
        // Phonetic Hindi Hinglish transliterations
        assertEquals("Potato", MandiTranslationUtils.translateHindiToEnglish("aloo"))
        assertEquals("Potato", MandiTranslationUtils.translateHindiToEnglish("alu"))
        assertEquals("Tomato", MandiTranslationUtils.translateHindiToEnglish("tamatar"))
        assertEquals("Onion", MandiTranslationUtils.translateHindiToEnglish("pyaz"))
        assertEquals("Bitter Gourd", MandiTranslationUtils.translateHindiToEnglish("karela"))

        // Case insensitivity
        assertEquals("आलू", MandiTranslationUtils.translateEnglishToHindi("pOTaTo"))
    }
}
