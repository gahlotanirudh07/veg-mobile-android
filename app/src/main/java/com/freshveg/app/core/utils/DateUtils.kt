package com.freshveg.app.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

fun String?.parseEpochMs(): Long {
    if (this == null || this.isBlank()) return 0L
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(this)
            if (date != null) {
                return date.time
            }
        } catch (_: Exception) {}
    }
    return 0L
}

fun String?.isToday(): Boolean {
    val ms = this.parseEpochMs()
    if (ms == 0L) {
        val dateStr = this.safeDate()
        val nowSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateStr == nowSdf.format(Date())
    }
    val calOrder = Calendar.getInstance().apply { timeInMillis = ms }
    val calNow = Calendar.getInstance()
    return calOrder.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calOrder.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
}

fun String?.isYesterday(): Boolean {
    val ms = this.parseEpochMs()
    val calNow = Calendar.getInstance()
    calNow.add(Calendar.DAY_OF_YEAR, -1)
    if (ms == 0L) {
        val dateStr = this.safeDate()
        val nowSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateStr == nowSdf.format(calNow.time)
    }
    val calOrder = Calendar.getInstance().apply { timeInMillis = ms }
    return calOrder.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calOrder.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
}

fun String?.isThisWeek(): Boolean {
    val ms = this.parseEpochMs()
    if (ms == 0L) return false
    val diff = System.currentTimeMillis() - ms
    return diff in 0..(7L * 24L * 3600L * 1000L)
}
