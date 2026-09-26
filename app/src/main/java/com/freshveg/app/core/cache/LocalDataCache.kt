package com.freshveg.app.core.cache

import android.content.Context
import com.freshveg.app.core.network.FreshnessConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance, stale-while-revalidate local cache.
 * Combines an in-memory concurrent map for instant (0ms) reads with persistent
 * disk caching in internal storage.
 *
 * Guarantees that users never see a blank screen or false "No Data" state
 * while Neon PostgreSQL or Render is waking up.
 */
@Singleton
class LocalDataCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        const val KEY_BUYER_PRODUCTS = "cache_buyer_products"
        const val KEY_BUYER_CATEGORIES = "cache_buyer_categories"
        const val KEY_BUYER_CONNECTED_SELLER = "cache_buyer_connected_seller"
        const val KEY_BUYER_CUTOFF = "cache_buyer_cutoff"
        const val KEY_BUYER_FREQUENT = "cache_buyer_frequent"
        const val KEY_BUYER_LAST_ORDER = "cache_buyer_last_order"
        const val KEY_BUYER_ORDERS = "cache_buyer_orders"
        const val KEY_BUYER_INVOICES = "cache_buyer_invoices"
        const val KEY_SELLER_ORDERS = "cache_seller_orders"
        const val KEY_SELLER_INVOICES = "cache_seller_invoices"
        const val KEY_SELLER_PENDING_ORDERS = "cache_seller_pending_orders"
        const val KEY_SELLER_RATES = "cache_seller_rates"
        const val KEY_SELLER_CATEGORIES = "cache_seller_categories"
    }

    private data class MemoryEntry(
        val parsedObject: Any,
        val timestamp: Long
    )

    private val memoryCache = ConcurrentHashMap<String, MemoryEntry>()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val cacheDir: File by lazy {
        File(context.cacheDir, "mandi_data_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Put an object into memory and persist to disk asynchronously.
     */
    fun <T : Any> put(key: String, data: T) {
        val now = System.currentTimeMillis()
        memoryCache[key] = MemoryEntry(parsedObject = data, timestamp = now)

        ioScope.launch {
            try {
                val file = File(cacheDir, "$key.json")
                val json = gson.toJson(data)
                file.writeText(json)
                val metaFile = File(cacheDir, "$key.meta")
                metaFile.writeText(now.toString())
            } catch (_: Exception) {
                // Disk write failure must not disrupt in-memory operations
            }
        }
    }

    /**
     * Retrieve an object from memory (0ms) or disk (<5ms).
     * Returns null if no cached data exists.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String, type: Type): T? {
        val mem = memoryCache[key]
        if (mem != null) {
            return mem.parsedObject as? T
        }

        // Try reading from disk
        return try {
            val file = File(cacheDir, "$key.json")
            if (file.exists()) {
                val json = file.readText()
                val parsed: T? = gson.fromJson(json, type)
                if (parsed != null) {
                    val metaFile = File(cacheDir, "$key.meta")
                    val timestamp = metaFile.takeIf { it.exists() }?.readText()?.toLongOrNull() ?: file.lastModified()
                    memoryCache[key] = MemoryEntry(parsedObject = parsed, timestamp = timestamp)
                    parsed
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retrieve an object from memory or disk using a Class token.
     */
    fun <T : Any> get(key: String, clazz: Class<T>): T? = get(key, clazz as Type)

    /**
     * Get the epoch millisecond timestamp when the cache key was last successfully updated.
     */
    fun getLastUpdated(key: String): Long {
        val mem = memoryCache[key]
        if (mem != null) return mem.timestamp

        return try {
            val metaFile = File(cacheDir, "$key.meta")
            if (metaFile.exists()) {
                metaFile.readText().toLongOrNull() ?: 0L
            } else {
                val file = File(cacheDir, "$key.json")
                if (file.exists()) file.lastModified() else 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Check if data for this key is completely fresh (< 30 seconds old by default).
     */
    fun isFresh(key: String, thresholdMs: Long = FreshnessConfig.freshThresholdMs): Boolean {
        val last = getLastUpdated(key)
        if (last == 0L) return false
        return (System.currentTimeMillis() - last) < thresholdMs
    }

    /**
     * Check if data is stale (> 5 minutes old by default).
     */
    fun isStale(key: String, thresholdMs: Long = FreshnessConfig.staleThresholdMs): Boolean {
        val last = getLastUpdated(key)
        if (last == 0L) return true
        return (System.currentTimeMillis() - last) >= thresholdMs
    }

    /**
     * Returns true if there is any cached data present for this key.
     */
    fun has(key: String): Boolean {
        if (memoryCache.containsKey(key)) return true
        return File(cacheDir, "$key.json").exists()
    }

    /**
     * Invalidate a specific cache key.
     */
    fun evict(key: String) {
        memoryCache.remove(key)
        ioScope.launch {
            try {
                File(cacheDir, "$key.json").delete()
                File(cacheDir, "$key.meta").delete()
            } catch (_: Exception) {}
        }
    }
}
