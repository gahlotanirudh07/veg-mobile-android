package com.freshveg.app.core.utils

data class ProduceTranslationEntry(
    val english: String,
    val hindi: String,
    val category: String,
    val aliases: List<String>
)

object MandiTranslationUtils {

    val DICTIONARY = listOf(
        // Root & Bulb Vegetables
        ProduceTranslationEntry("Potato", "आलू", "Root Veggies", listOf("aloo", "alu", "aalo", "aaloo", "potato", "potatoes", "jyoti")),
        ProduceTranslationEntry("Sweet Potato", "शकरकंद", "Root Veggies", listOf("shakarkand")),
        ProduceTranslationEntry("Onion", "प्याज", "Root Veggies", listOf("pyaz", "pyaaz", "onion", "onions")),
        ProduceTranslationEntry("Red Onion", "लाल प्याज", "Root Veggies", listOf("lal pyaz")),
        ProduceTranslationEntry("White Onion", "सफेद प्याज", "Root Veggies", listOf("safed pyaz")),
        ProduceTranslationEntry("Spring Onion", "हरा प्याज", "Leafy Greens", listOf("hara pyaz", "scallion")),
        ProduceTranslationEntry("Garlic", "लहसुन", "Aromatics", listOf("lahsun", "lehsun", "garlic")),
        ProduceTranslationEntry("Ginger", "अदरक", "Aromatics", listOf("adrak", "adrakh", "ginger")),
        ProduceTranslationEntry("Turmeric", "कच्ची हल्दी", "Aromatics", listOf("haldi", "kacchi haldi")),
        ProduceTranslationEntry("Beetroot", "चुकंदर", "Root Veggies", listOf("chukandar", "beet")),
        ProduceTranslationEntry("Radish", "मूली", "Root Veggies", listOf("mooli", "muli")),
        ProduceTranslationEntry("Red Radish", "लाल मूली", "Root Veggies", listOf("lal mooli")),
        ProduceTranslationEntry("Carrot", "गाजर", "Root Veggies", listOf("gajar", "carrots")),
        ProduceTranslationEntry("Red Carrot", "लाल गाजर", "Root Veggies", listOf("lal gajar")),
        ProduceTranslationEntry("Colocasia / Arbi", "अरबी", "Root Veggies", listOf("arbi", "arbhi", "ghuiya")),
        ProduceTranslationEntry("Yam / Suran", "सूरन (जिमीकंद)", "Root Veggies", listOf("suran", "jimikand")),

        // Fruity & Nightshade Vegetables
        ProduceTranslationEntry("Tomato", "टमाटर", "Fruity Veggies", listOf("tamatar", "tomatoes", "tomato")),
        ProduceTranslationEntry("Hybrid Tomato", "हाइब्रिड टमाटर", "Fruity Veggies", listOf("hybrid tamatar")),
        ProduceTranslationEntry("Desi Tomato", "देशी टमाटर", "Fruity Veggies", listOf("desi tamatar")),
        ProduceTranslationEntry("Cherry Tomato", "चेरी टमाटर", "Exotic Veg", listOf("cherry tamatar")),
        ProduceTranslationEntry("Brinjal / Eggplant", "बैंगन", "Fruity Veggies", listOf("baingan", "baigan", "eggplant", "brinjal")),
        ProduceTranslationEntry("Long Brinjal", "लंबा बैंगन", "Fruity Veggies", listOf("lamba baingan")),
        ProduceTranslationEntry("Bharta Brinjal", "भरता बैंगन", "Fruity Veggies", listOf("bharta baingan", "gol baingan", "bharta", "bharta baigan")),
        ProduceTranslationEntry("Capsicum", "शिमला मिर्च", "Fruity Veggies", listOf("shimla mirch", "capsicum", "bell pepper")),
        ProduceTranslationEntry("Green Chilli", "हरी मिर्च", "Aromatics", listOf("hari mirch", "green chilli", "chili")),
        ProduceTranslationEntry("Spicy Green Chilli", "तीखी हरी मिर्च", "Aromatics", listOf("teekhi mirch")),
        ProduceTranslationEntry("Okra / Lady Finger", "भिंडी", "Fruity Veggies", listOf("bhindi", "okra", "lady finger")),

        // Gourds & Pods
        ProduceTranslationEntry("Bottle Gourd", "लौकी", "Gourds & Pods", listOf("lauki", "ghiya", "dudhi")),
        ProduceTranslationEntry("Bitter Gourd", "करेला", "Gourds & Pods", listOf("karela", "bitter gourd")),
        ProduceTranslationEntry("Ridge Gourd", "तोरई", "Gourds & Pods", listOf("torai", "tori", "turai")),
        ProduceTranslationEntry("Sponge Gourd", "नेनुआ", "Gourds & Pods", listOf("nenua", "ghiya torai")),
        ProduceTranslationEntry("Pointed Gourd", "परवल", "Gourds & Pods", listOf("parwal")),
        ProduceTranslationEntry("Ivy Gourd / Kundru", "कुंदरू", "Gourds & Pods", listOf("kundru", "tendli", "tindora")),
        ProduceTranslationEntry("Pumpkin", "कद्दू (सीताफल)", "Gourds & Pods", listOf("kaddu", "sitaphal", "kashiphal")),
        ProduceTranslationEntry("Cucumber", "खीरा", "Salads", listOf("kheera", "cucumber")),
        ProduceTranslationEntry("Kakdi", "ककड़ी", "Salads", listOf("kakdi")),
        ProduceTranslationEntry("French Beans", "फ्रेंच बीन्स", "Gourds & Pods", listOf("french beans", "beans")),
        ProduceTranslationEntry("Cluster Beans / Gawar", "गवार फली", "Gourds & Pods", listOf("gawar phali", "guar")),
        ProduceTranslationEntry("Flat Beans / Sem", "सेम फली", "Gourds & Pods", listOf("sem phali", "sem")),
        ProduceTranslationEntry("Green Peas", "हरी मटर", "Gourds & Pods", listOf("matar", "green peas", "mutter")),
        ProduceTranslationEntry("Drumstick", "सहजन (मुनगा)", "Gourds & Pods", listOf("sahjan", "munga", "drumstick")),
        ProduceTranslationEntry("Raw Jackfruit", "कच्चा कटहल", "Gourds & Pods", listOf("kathal", "jackfruit")),
        ProduceTranslationEntry("Raw Banana", "कच्चा केला", "Gourds & Pods", listOf("kacha kela", "raw plantain")),
        ProduceTranslationEntry("Raw Papaya", "कच्चा पपीता", "Gourds & Pods", listOf("kacha papita", "raw papaya")),
        ProduceTranslationEntry("Raw Mango / Kairi", "कच्ची कैरी", "Gourds & Pods", listOf("kairi", "kacchi kairi", "aambi")),

        // Cruciferous
        ProduceTranslationEntry("Cauliflower", "फूलगोभी", "Cruciferous", listOf("phool gobhi", "gobhi", "cauliflower")),
        ProduceTranslationEntry("Cabbage", "पत्तागोभी", "Cruciferous", listOf("patta gobhi", "bandh gobhi", "cabbage")),
        ProduceTranslationEntry("Purple Cabbage", "लाल पत्तागोभी", "Exotic Veg", listOf("red cabbage", "lal gobhi")),
        ProduceTranslationEntry("Broccoli", "ब्रोकली", "Exotic Veg", listOf("broccoli", "hari gobhi")),

        // Leafy Greens & Aromatics
        ProduceTranslationEntry("Spinach", "पालक", "Leafy Greens", listOf("palak", "spinach")),
        ProduceTranslationEntry("Coriander Leaves", "धनिया पत्ती", "Aromatics", listOf("dhaniya", "coriander", "cilantro")),
        ProduceTranslationEntry("Fenugreek Leaves", "मेथी", "Leafy Greens", listOf("methi", "fenugreek")),
        ProduceTranslationEntry("Mint Leaves", "पुदीना", "Aromatics", listOf("pudina", "mint")),
        ProduceTranslationEntry("Mustard Greens", "सरसों का साग", "Leafy Greens", listOf("sarson saag", "sarson")),
        ProduceTranslationEntry("Curry Leaves", "कढ़ी पत्ता", "Aromatics", listOf("kadi patta", "curry patta", "meetha neem")),
        ProduceTranslationEntry("Bathua", "बथुआ साग", "Leafy Greens", listOf("bathua")),
        ProduceTranslationEntry("Cholai Saag", "चौलाई साग", "Leafy Greens", listOf("chaulai", "cholai")),
        ProduceTranslationEntry("Lemon", "नींबू", "Aromatics", listOf("nimbu", "lemon", "lime")),
        ProduceTranslationEntry("Mushroom", "मशरूम", "Exotic Veg", listOf("mushroom", "dhingri", "khumbh")),
        ProduceTranslationEntry("Sweet Corn", "स्वीट कॉर्न", "Exotic Veg", listOf("corn", "bhutta", "makka"))
    )

    /**
     * Translates Hindi text (or Hindi voice input) to standard English produce name.
     */
    fun translateHindiToEnglish(hindiInput: String): String? {
        val clean = hindiInput.trim()
        if (clean.isBlank()) return null

        // 1. Direct or partial Hindi match
        val direct = DICTIONARY.find { entry ->
            entry.hindi.contains(clean, ignoreCase = true) || clean.contains(entry.hindi, ignoreCase = true)
        }
        if (direct != null) return direct.english

        // 2. Transliterated / alias match
        val lower = clean.lowercase()
        val aliasMatch = DICTIONARY.find { entry ->
            entry.aliases.any { alias -> alias.contains(lower) || lower.contains(alias) }
        }
        return aliasMatch?.english
    }

    /**
     * Translates English or Romanized Hindi input (e.g. "palak", "tomato", "tamatar") to standard Hindi script.
     */
    fun translateEnglishToHindi(englishInput: String): String? {
        val clean = englishInput.trim().lowercase()
        if (clean.isBlank()) return null

        // 1. Exact or partial English name match
        val direct = DICTIONARY.find { entry ->
            entry.english.lowercase().contains(clean) || clean.contains(entry.english.lowercase())
        }
        if (direct != null) return direct.hindi

        // 2. Alias / transliterated match (e.g. "tamatar" -> "टमाटर")
        val aliasMatch = DICTIONARY.find { entry ->
            entry.aliases.any { alias -> alias.contains(clean) || clean.contains(alias) }
        }
        return aliasMatch?.hindi
    }
}
