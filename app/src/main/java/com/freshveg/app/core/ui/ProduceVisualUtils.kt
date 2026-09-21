package com.freshveg.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.Locale

object ProduceVisualUtils {

    fun isHindi(): Boolean {
        val managerLang = com.freshveg.app.core.i18n.LanguageManager.instance?.currentLanguage?.value
        if (managerLang != null) {
            return managerLang == com.freshveg.app.core.i18n.AppLanguage.HINDI
        }
        return java.util.Locale.getDefault().language == "hi"
    }

    fun getProduceDisplayName(name: String?, hindiName: String? = null, isHindi: Boolean = isHindi()): String {
        if (isHindi) {
            if (!hindiName.isNullOrBlank()) return hindiName
            val translated = com.freshveg.app.core.utils.MandiTranslationUtils.translateEnglishToHindi(name ?: "")
            if (!translated.isNullOrBlank()) return translated
        }
        return name ?: ""
    }

    fun getProduceSecondaryName(name: String?, hindiName: String? = null, isHindi: Boolean = isHindi()): String? {
        val hi = if (!hindiName.isNullOrBlank()) hindiName else com.freshveg.app.core.utils.MandiTranslationUtils.translateEnglishToHindi(name ?: "")
        return if (isHindi) {
            if (!name.isNullOrBlank() && name != hi) name else null
        } else {
            if (!hi.isNullOrBlank() && hi != name) hi else null
        }
    }

    fun getCategoryDisplayName(name: String?, isHindi: Boolean = isHindi()): String {
        if (!isHindi) return name ?: ""
        val safe = name?.trim() ?: ""
        return when {
            safe.contains("Fruiting", ignoreCase = true) || safe.contains("फलदार", ignoreCase = true) -> "फलदार सब्जियां"
            safe.contains("Root", ignoreCase = true) || safe.contains("Tuber", ignoreCase = true) || safe.contains("जड़", ignoreCase = true) -> "जड़ वाली सब्जियां"
            safe.contains("Leafy", ignoreCase = true) || safe.contains("Greens", ignoreCase = true) || safe.contains("पत्तेदार", ignoreCase = true) -> "पत्तेदार सब्जियां"
            safe.contains("Gourd", ignoreCase = true) || safe.contains("Squash", ignoreCase = true) || safe.contains("बेल", ignoreCase = true) -> "बेल वाली सब्जियां"
            safe.contains("Exotic", ignoreCase = true) || safe.contains("Continental", ignoreCase = true) || safe.contains("विदेशी", ignoreCase = true) -> "विदेशी सब्जियां"
            safe.contains("Fruit", ignoreCase = true) || safe.contains("फल", ignoreCase = true) -> "ताजे फल"
            safe.contains("Herb", ignoreCase = true) || safe.contains("Seasoning", ignoreCase = true) || safe.contains("मसाले", ignoreCase = true) -> "मसाले और हर्ब्स"
            safe.equals("All", ignoreCase = true) || safe.equals("All Produce", ignoreCase = true) -> "सभी सब्जियां"
            else -> com.freshveg.app.core.utils.MandiTranslationUtils.translateEnglishToHindi(safe) ?: safe
        }
    }

    fun getUnitDisplayName(unit: String?, isHindi: Boolean = isHindi()): String {
        if (!isHindi) return unit?.lowercase() ?: "kg"
        return when (unit?.lowercase()?.trim()) {
            "kg", "kilogram" -> "किलो"
            "crate" -> "क्रेट"
            "bunch" -> "गुच्छा"
            "piece", "pc", "pcs" -> "पीस"
            "packet", "pkt" -> "पैकेट"
            "sack", "bag", "bori" -> "बोरी"
            "gram", "gm", "g" -> "ग्राम"
            else -> unit?.lowercase() ?: "किलो"
        }
    }

    fun getOrderStatusDisplayName(status: String?, isHindi: Boolean = isHindi()): String {
        if (!isHindi) return status ?: ""
        return when (status?.uppercase()?.trim()) {
            "PENDING" -> "लंबित"
            "CONFIRMED" -> "स्वीकृत"
            "FULFILLED", "DELIVERED" -> "डिस्पैच / डिलीवर"
            "CANCELLED" -> "रद्द"
            else -> status ?: ""
        }
    }

    fun formatQuantity(qty: Double, unit: String = "kg"): String {
        val formattedNumber = if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
        }
        return "$formattedNumber $unit"
    }

    fun formatQuantityValue(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
        }
    }

    fun formatCurrency(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "₹${amount.toInt()}"
        } else {
            "₹${String.format(Locale.US, "%.2f", amount)}"
        }
    }

    fun getProduceEmoji(name: String?, hindiName: String? = null): String {
        val safeName = name?.lowercase() ?: ""
        val safeHindi = hindiName?.lowercase() ?: ""
        val query = "$safeName $safeHindi".trim()
        if (query.isEmpty()) return "🥬"

        return when {
            query.contains("tomato") || query.contains("tamatar") || query.contains("टमाटर") -> "🍅"
            query.contains("potato") || query.contains("aloo") || query.contains("आलू") -> "🥔"
            query.contains("onion") || query.contains("pyaz") || query.contains("प्याज") -> "🧅"
            query.contains("garlic") || query.contains("lahsun") || query.contains("lehsun") || query.contains("लहसुन") -> "🧄"
            query.contains("ginger") || query.contains("adrak") || query.contains("अदरक") -> "🫚"
            query.contains("chilli") || query.contains("chili") || query.contains("mirch") || query.contains("मिर्च") -> "🌶️"
            query.contains("capsicum") || query.contains("shimla") || query.contains("शिमला") || query.contains("bell pepper") -> "🫑"
            query.contains("spinach") || query.contains("palak") || query.contains("पालक") || query.contains("methi") || query.contains("मेथी") || query.contains("saag") || query.contains("साग") -> "🥬"
            query.contains("coriander") || query.contains("dhaniya") || query.contains("धनिया") || query.contains("cilantro") || query.contains("mint") || query.contains("pudina") || query.contains("पुदीना") || query.contains("curry") || query.contains("कढ़ी") -> "🌿"
            query.contains("carrot") || query.contains("gajar") || query.contains("गाजर") -> "🥕"
            query.contains("radish") || query.contains("mooli") || query.contains("मूली") || query.contains("beetroot") || query.contains("chukandar") || query.contains("चुकंदर") -> "🥗"
            query.contains("brinjal") || query.contains("eggplant") || query.contains("baingan") || query.contains("बैंगन") -> "🍆"
            query.contains("cauliflower") || query.contains("gobhi") || query.contains("गोभी") || query.contains("cabbage") || query.contains("patta") || query.contains("पत्तागोभी") || query.contains("broccoli") || query.contains("ब्रोकली") -> "🥦"
            query.contains("lemon") || query.contains("nimbu") || query.contains("नींबू") -> "🍋"
            query.contains("cucumber") || query.contains("kheera") || query.contains("खीरा") || query.contains("kakdi") || query.contains("ककड़ी") || query.contains("lauki") || query.contains("लौकी") || query.contains("karela") || query.contains("करेला") || query.contains("torai") || query.contains("तोरई") || query.contains("gourd") -> "🥒"
            query.contains("corn") || query.contains("bhutta") || query.contains("मक्का") -> "🌽"
            query.contains("pea") || query.contains("matar") || query.contains("मटर") || query.contains("bean") || query.contains("sem") || query.contains("gawar") || query.contains("गवार") || query.contains("phali") || query.contains("फली") -> "🫛"
            query.contains("mushroom") || query.contains("मशरूम") -> "🍄"
            query.contains("pumpkin") || query.contains("kaddu") || query.contains("कद्दू") || query.contains("sitaphal") || query.contains("सीताफल") -> "🎃"
            query.contains("banana") || query.contains("kela") || query.contains("केला") -> "🍌"
            query.contains("papaya") || query.contains("papita") || query.contains("पपीता") -> "🍈"
            query.contains("mango") || query.contains("kairi") || query.contains("आम") || query.contains("कैरी") -> "🥭"
            query.contains("apple") || query.contains("seb") || query.contains("सेब") -> "🍎"
            else -> "🥬"
        }
    }

    fun getProduceAssetPath(name: String?, hindiName: String? = null, imageUrl: String? = null): String {
        if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("http")) {
            return imageUrl
        }
        val safeName = name?.lowercase() ?: ""
        val safeHindi = hindiName?.lowercase() ?: ""
        val query = "$safeName $safeHindi".trim()

        return when {
            query.contains("tomato") || query.contains("tamatar") || query.contains("टमाटर") -> "file:///android_asset/produce/tomato.svg"
            query.contains("potato") || query.contains("aloo") || query.contains("आलू") -> "file:///android_asset/produce/potato.svg"
            query.contains("onion") || query.contains("pyaz") || query.contains("प्याज") -> "file:///android_asset/produce/onion.svg"
            query.contains("garlic") || query.contains("lahsun") || query.contains("lehsun") || query.contains("लहसुन") -> "file:///android_asset/produce/garlic.svg"
            query.contains("ginger") || query.contains("adrak") || query.contains("अदरक") -> "file:///android_asset/produce/ginger.svg"
            query.contains("chilli") || query.contains("chili") || query.contains("mirch") || query.contains("मिर्च") -> "file:///android_asset/produce/chilli.svg"
            query.contains("capsicum") || query.contains("shimla") || query.contains("शिमला") || query.contains("bell pepper") -> "file:///android_asset/produce/capsicum.svg"
            query.contains("spinach") || query.contains("palak") || query.contains("पालक") -> "file:///android_asset/produce/spinach.svg"
            query.contains("coriander") || query.contains("dhaniya") || query.contains("धनिया") || query.contains("cilantro") -> "file:///android_asset/produce/coriander.svg"
            query.contains("mint") || query.contains("pudina") || query.contains("पुदीना") -> "file:///android_asset/produce/mint.svg"
            query.contains("carrot") || query.contains("gajar") || query.contains("गाजर") -> "file:///android_asset/produce/carrot.svg"
            query.contains("radish") || query.contains("mooli") || query.contains("मूली") -> "file:///android_asset/produce/radish.svg"
            query.contains("beetroot") || query.contains("chukandar") || query.contains("चुकंदर") -> "file:///android_asset/produce/beetroot.svg"
            query.contains("brinjal") || query.contains("eggplant") || query.contains("baingan") || query.contains("बैंगन") -> "file:///android_asset/produce/brinjal.svg"
            query.contains("cauliflower") || query.contains("phool") || query.contains("फूलगोभी") -> "file:///android_asset/produce/cauliflower.svg"
            query.contains("cabbage") || query.contains("patta") || query.contains("पत्तागोभी") -> "file:///android_asset/produce/cabbage.svg"
            query.contains("broccoli") || query.contains("ब्रोकली") -> "file:///android_asset/produce/broccoli.svg"
            query.contains("lemon") || query.contains("nimbu") || query.contains("नींबू") -> "file:///android_asset/produce/lemon.svg"
            query.contains("cucumber") || query.contains("kheera") || query.contains("खीरा") || query.contains("kakdi") || query.contains("ककड़ी") -> "file:///android_asset/produce/cucumber.svg"
            query.contains("bottle gourd") || query.contains("lauki") || query.contains("लौकी") || query.contains("ghia") -> "file:///android_asset/produce/bottle_gourd.svg"
            query.contains("bitter gourd") || query.contains("karela") || query.contains("करेला") -> "file:///android_asset/produce/bitter_gourd.svg"
            query.contains("ridge gourd") || query.contains("torai") || query.contains("turai") || query.contains("तोरई") -> "file:///android_asset/produce/ridge_gourd.svg"
            query.contains("sponge gourd") || query.contains("nenua") || query.contains("नेनुआ") -> "file:///android_asset/produce/sponge_gourd.svg"
            query.contains("pointed gourd") || query.contains("parwal") || query.contains("परवल") -> "file:///android_asset/produce/pointed_gourd.svg"
            query.contains("ivy gourd") || query.contains("kundru") || query.contains("tindora") || query.contains("कुंदरू") -> "file:///android_asset/produce/ivy_gourd.svg"
            query.contains("ladyfinger") || query.contains("okra") || query.contains("bhindi") || query.contains("भिंडी") -> "file:///android_asset/produce/okra.svg"
            query.contains("fenugreek") || query.contains("methi") || query.contains("मेथी") -> "file:///android_asset/produce/fenugreek.svg"
            query.contains("mustard") || query.contains("sarson") || query.contains("सरसों") -> "file:///android_asset/produce/mustard_greens.svg"
            query.contains("curry") || query.contains("kadi patta") || query.contains("कढ़ी पत्ता") -> "file:///android_asset/produce/curry_leaves.svg"
            query.contains("corn") || query.contains("bhutta") || query.contains("मक्का") -> "file:///android_asset/produce/corn.svg"
            query.contains("pea") || query.contains("matar") || query.contains("मटर") -> "file:///android_asset/produce/green_peas.svg"
            query.contains("green bean") || query.contains("french bean") || query.contains("beans") || query.contains("फली") -> "file:///android_asset/produce/green_beans.svg"
            query.contains("cluster bean") || query.contains("gawar") || query.contains("गवार") -> "file:///android_asset/produce/cluster_beans.svg"
            query.contains("mushroom") || query.contains("मशरूम") -> "file:///android_asset/produce/mushroom.svg"
            query.contains("pumpkin") || query.contains("kaddu") || query.contains("कद्दू") || query.contains("sitaphal") || query.contains("सीताफल") -> "file:///android_asset/produce/pumpkin.svg"
            query.contains("banana") || query.contains("kela") || query.contains("केला") -> "file:///android_asset/produce/banana.svg"
            query.contains("papaya") || query.contains("papita") || query.contains("पपीता") -> "file:///android_asset/produce/papaya.svg"
            query.contains("mango") || query.contains("kairi") || query.contains("आम") -> "file:///android_asset/produce/mango.svg"
            query.contains("apple") || query.contains("seb") || query.contains("सेब") -> "file:///android_asset/produce/apple.svg"
            query.contains("watermelon") || query.contains("tarbooj") || query.contains("तरबूज") -> "file:///android_asset/produce/watermelon.svg"
            query.contains("grapes") || query.contains("angoor") || query.contains("अंगूर") -> "file:///android_asset/produce/grapes.svg"
            query.contains("orange") || query.contains("santra") || query.contains("संतरा") -> "file:///android_asset/produce/orange.svg"
            query.contains("pineapple") || query.contains("ananas") || query.contains("अनानास") -> "file:///android_asset/produce/pineapple.svg"
            query.contains("pomegranate") || query.contains("anar") || query.contains("अनार") -> "file:///android_asset/produce/pomegranate.svg"
            query.contains("guava") || query.contains("amrood") || query.contains("अमरूद") -> "file:///android_asset/produce/guava.svg"
            query.contains("kathal") || query.contains("jackfruit") || query.contains("कटहल") -> "file:///android_asset/produce/kathal.svg"
            else -> "file:///android_asset/produce/general.svg"
        }
    }

    fun getProduceBgColor(name: String?, hindiName: String? = null): Color {
        val safeName = name?.lowercase() ?: ""
        val safeHindi = hindiName?.lowercase() ?: ""
        val query = "$safeName $safeHindi".trim()

        return when {
            query.contains("tomato") || query.contains("tamatar") || query.contains("chilli") || query.contains("mirch") || query.contains("apple") || query.contains("pomegranate") -> Color(0xFFFFEBEE)
            query.contains("potato") || query.contains("aloo") || query.contains("onion") || query.contains("pyaz") || query.contains("ginger") || query.contains("adrak") || query.contains("corn") || query.contains("banana") -> Color(0xFFFFF8E1)
            query.contains("brinjal") || query.contains("baingan") || query.contains("beetroot") || query.contains("chukandar") || query.contains("grapes") -> Color(0xFFF3E5F5)
            query.contains("carrot") || query.contains("gajar") || query.contains("pumpkin") || query.contains("kaddu") || query.contains("papaya") || query.contains("orange") -> Color(0xFFFFF3E0)
            query.contains("lemon") || query.contains("nimbu") || query.contains("pineapple") -> Color(0xFFFFFDE7)
            else -> Color(0xFFE8F5E9)
        }
    }
}

@Composable
fun ProduceThumbnailBadge(
    name: String?,
    hindiName: String? = null,
    imageUrl: String? = null,
    size: Dp = 60.dp,
    cornerRadius: Dp = 10.dp,
    useUnifiedBackground: Boolean = true,
    modifier: Modifier = Modifier
) {
    val modelUri = ProduceVisualUtils.getProduceAssetPath(name, hindiName, imageUrl)
    val containerBg = if (useUnifiedBackground) Color(0xFFF7F8F5) else ProduceVisualUtils.getProduceBgColor(name, hindiName)
    val containerBorder = if (useUnifiedBackground) Color(0xFFE5E9E2) else Color(0x14000000)
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerBg)
            .border(BorderStroke(0.75.dp, containerBorder), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        // High density image fill: minimal 1dp padding so vector produce art occupies >95% of badge area
        val innerPadding = if (size >= 56.dp) 1.dp else 0.5.dp
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(modelUri)
                .crossfade(true)
                .build(),
            contentDescription = name ?: "Produce",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

