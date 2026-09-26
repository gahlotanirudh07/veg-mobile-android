package com.freshveg.app.core.network

/**
 * Centralized, configurable freshness thresholds for MandiExpress data.
 * Used across ViewModels and lifecycle monitors to avoid unnecessary database wakeups.
 */
object FreshnessConfig {
    const val FRESH_THRESHOLD_MS: Long = 30_000L
    const val STALE_THRESHOLD_MS: Long = 300_000L
    const val MODERATE_STALE_THRESHOLD_MS: Long = 60_000L
    const val SLOW_CONNECTION_THRESHOLD_MS: Long = 2_500L

    /**
     * Data younger than this threshold (< 30 seconds) is considered completely fresh.
     * When the app resumes, requests will NOT hit the network if data age < 30s.
     */
    var freshThresholdMs: Long = FRESH_THRESHOLD_MS

    /**
     * Data older than this threshold (> 5 minutes) is considered stale.
     * When the app resumes, a background refresh is triggered immediately.
     */
    var staleThresholdMs: Long = STALE_THRESHOLD_MS

    /**
     * Between 30s and 5m, optional background refresh can be performed for high-importance screens.
     */
    var moderateStaleThresholdMs: Long = MODERATE_STALE_THRESHOLD_MS

    /**
     * If an API call / pull-to-refresh takes longer than this threshold,
     * the UI transitions to a "Connecting to live database..." state so the user
     * knows Neon PostgreSQL is booting up.
     */
    var slowConnectionThresholdMs: Long = SLOW_CONNECTION_THRESHOLD_MS
}
