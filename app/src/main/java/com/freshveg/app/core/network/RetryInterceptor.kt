package com.freshveg.app.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    companion object {
        private const val TAG = "RetryInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        var tryCount = 0
        val maxRetries = NetworkConfig.maxRetries

        while (tryCount < maxRetries) {
            tryCount++
            try {
                response?.close()
                response = chain.proceed(request)

                // Success or non-retryable client error (4xx except 429) -> return immediately
                if (response.isSuccessful || (response.code in 400..499 && response.code != 429)) {
                    return response
                }

                // If transient server error (502, 503, 504, 429) and retries remain -> backoff & retry
                if (response.code in NetworkConfig.retryableStatusCodes && tryCount < maxRetries) {
                    val serverRetryAfterMs = response.header("Retry-After")?.toLongOrNull()?.let { it * 1000L }
                    val exponentialBackoffMs = (NetworkConfig.initialDelayMs * (1L shl (tryCount - 1)))
                        .coerceAtMost(NetworkConfig.maxDelayMs)
                    val delayMs = serverRetryAfterMs ?: exponentialBackoffMs

                    Log.w(TAG, "Transient HTTP ${response.code} (DB/Server waking). Retrying in ${delayMs}ms (attempt $tryCount of $maxRetries)...")
                    try {
                        Thread.sleep(delayMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return response
                    }
                    continue
                }

                return response
            } catch (e: IOException) {
                lastException = e
                if (e is SocketTimeoutException || e is ConnectException || e is UnknownHostException) {
                    if (tryCount < maxRetries) {
                        val delayMs = (NetworkConfig.initialDelayMs * (1L shl (tryCount - 1)))
                            .coerceAtMost(NetworkConfig.maxDelayMs)
                        Log.w(TAG, "Transient network error (${e.javaClass.simpleName}). Retrying in ${delayMs}ms (attempt $tryCount of $maxRetries)...")
                        try {
                            Thread.sleep(delayMs)
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                            throw e
                        }
                        continue
                    }
                }
                throw e
            }
        }

        return response ?: throw (lastException ?: IOException("Request failed after $maxRetries attempts"))
    }
}
