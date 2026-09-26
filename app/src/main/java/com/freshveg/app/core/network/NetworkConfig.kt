package com.freshveg.app.core.network

/**
 * Centralized, configurable network policy for OkHttp & Retrofit.
 * Controls timeouts, exponential backoff, and retryable conditions for transient Neon/Render cold starts.
 */
object NetworkConfig {
    /**
     * Maximum retry attempts for transient server wake-up failures.
     */
    var maxRetries: Int = 3

    /**
     * Initial backoff delay in milliseconds before the first retry.
     */
    var initialDelayMs: Long = 1_500L

    /**
     * Maximum delay cap in milliseconds between retries.
     */
    var maxDelayMs: Long = 5_000L

    /**
     * HTTP status codes that represent transient infrastructure booting states
     * (e.g., Render container starting up, Neon PostgreSQL waking up, rate limiter cooloff).
     * 4xx client errors (400, 401, 403, 404, 422) are strictly NOT retried.
     */
    var retryableStatusCodes: Set<Int> = setOf(502, 503, 504, 429)

    /**
     * OkHttp connection timeout in seconds.
     * Neon DB wake-up + Render free-tier spin-up typically takes ~8-15 seconds.
     * 30 seconds provides sufficient headroom without triggering early client SocketTimeoutExceptions.
     */
    var connectTimeoutSeconds: Long = 30L

    /**
     * OkHttp read timeout in seconds.
     */
    var readTimeoutSeconds: Long = 30L

    /**
     * OkHttp write timeout in seconds.
     */
    var writeTimeoutSeconds: Long = 30L
}
