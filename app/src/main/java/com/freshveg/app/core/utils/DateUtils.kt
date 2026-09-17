package com.freshveg.app.core.utils

/**
 * Safe date substring helper that prevents NullPointerException and IndexOutOfBoundsException
 * across all Android screens when rendering API dates.
 */
fun String?.safeDate(fallback: String = "N/A"): String {
    if (this == null || this.isBlank()) return fallback
    return try {
        if (this.length >= 10) this.substring(0, 10) else this
    } catch (e: Exception) {
        fallback
    }
}

fun formatSafeDate(date1: String?, date2: String?, fallback: String = "N/A"): String {
    val chosen = if (!date1.isNullOrBlank()) date1 else date2
    return chosen.safeDate(fallback)
}
