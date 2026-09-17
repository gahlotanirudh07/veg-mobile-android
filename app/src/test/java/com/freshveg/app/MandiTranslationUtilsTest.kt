package com.freshveg.app

import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.utils.MandiTranslationUtils
import org.junit.Assert.*
import org.junit.Test

class MandiTranslationUtilsTest {

    @Test
    fun testHindiToEnglishAutoTranslation() {
        assertEquals("Tomato", MandiTranslationUtils.translateHindiToEnglish("टमाटर"))
        assertEquals("Potato", MandiTranslationUtils.translateHindiToEnglish("आलू"))
        assertEquals("Onion", MandiTranslationUtils.translateHindiToEnglish("प्याज"))
        assertEquals("Spinach", MandiTranslationUtils.translateHindiToEnglish("पालक"))
        assertEquals("Ginger", MandiTranslationUtils.translateHindiToEnglish("अदरक"))
        assertEquals("Garlic", MandiTranslationUtils.translateHindiToEnglish("लहसुन"))
        assertEquals("Green Chilli", MandiTranslationUtils.translateHindiToEnglish("हरी मिर्च"))
        assertEquals("Cauliflower", MandiTranslationUtils.translateHindiToEnglish("फूलगोभी"))
    }

    @Test
    fun testEnglishToHindiAutoTranslation() {
        assertEquals("टमाटर", MandiTranslationUtils.translateEnglishToHindi("Tomato"))
        assertEquals("आलू", MandiTranslationUtils.translateEnglishToHindi("Potato"))
        assertEquals("प्याज", MandiTranslationUtils.translateEnglishToHindi("Onion"))
        assertEquals("पालक", MandiTranslationUtils.translateEnglishToHindi("Spinach"))
        assertEquals("धनिया पत्ती", MandiTranslationUtils.translateEnglishToHindi("Coriander Leaves"))
        assertEquals("गाजर", MandiTranslationUtils.translateEnglishToHindi("Carrot"))
    }

    @Test
    fun testHinglishTransliterationSearch() {
        // Search by transliterated aliases
        val tomatoHindi = MandiTranslationUtils.translateEnglishToHindi("tamatar")
        assertEquals("टमाटर", tomatoHindi)

        val alooEnglish = MandiTranslationUtils.translateHindiToEnglish("aloo")
        assertEquals("Potato", alooEnglish)

        val pyazHindi = MandiTranslationUtils.translateEnglishToHindi("pyaz")
        assertEquals("प्याज", pyazHindi)

        val mirchiEnglish = MandiTranslationUtils.translateHindiToEnglish("hari mirch")
        assertEquals("Green Chilli", mirchiEnglish)
    }

    @Test
    fun testProduceEmojiMapping() {
        assertEquals("🍅", ProduceVisualUtils.getProduceEmoji("Tomato", null))
        assertEquals("🥔", ProduceVisualUtils.getProduceEmoji("Potato", null))
        assertEquals("🧅", ProduceVisualUtils.getProduceEmoji("Onion", null))
        assertEquals("🥬", ProduceVisualUtils.getProduceEmoji("Spinach", null))
        assertEquals("🌶️", ProduceVisualUtils.getProduceEmoji("Green Chilli", null))
        assertEquals("🥕", ProduceVisualUtils.getProduceEmoji("Carrot", null))
        assertEquals("🥦", ProduceVisualUtils.getProduceEmoji("Broccoli", null))
        assertEquals("🧄", ProduceVisualUtils.getProduceEmoji("Garlic", null))
        assertEquals("🫚", ProduceVisualUtils.getProduceEmoji("Ginger", null))
    }

    @Test
    fun testDictionaryCompletenessAndCategories() {
        val dictionary = MandiTranslationUtils.DICTIONARY
        assertTrue("Expected at least 30 produce items in dictionary", dictionary.size >= 30)

        val categories = dictionary.map { it.category }.distinct()
        assertTrue(categories.contains("Root Veggies"))
        assertTrue(categories.contains("Fruity Veggies"))
        assertTrue(categories.contains("Gourds & Pods"))
        assertTrue(categories.contains("Leafy Greens"))
        assertTrue(categories.contains("Aromatics"))
    }
}
