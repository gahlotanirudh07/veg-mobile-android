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
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1500L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        var tryCount = 0

        while (tryCount < MAX_RETRIES) {
            tryCount++
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.isSuccessful || (response.code in 400..499 && response.code != 429)) {
                    return response
                }

                if (response.code in listOf(502, 503, 504, 429) && tryCount < MAX_RETRIES) {
                    Log.w(TAG, "Server returned HTTP ${response.code}. Retrying in ${INITIAL_DELAY_MS * tryCount}ms (attempt $tryCount of $MAX_RETRIES)...")
                    Thread.sleep(INITIAL_DELAY_MS * tryCount)
                    continue
                }

                return response
            } catch (e: IOException) {
                lastException = e
                if (e is SocketTimeoutException || e is ConnectException || e is UnknownHostException) {
                    if (tryCount < MAX_RETRIES) {
                        Log.w(TAG, "Network transient error (${e.javaClass.simpleName}). Retrying in ${INITIAL_DELAY_MS * tryCount}ms (attempt $tryCount of $MAX_RETRIES)...")
                        try {
                            Thread.sleep(INITIAL_DELAY_MS * tryCount)
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

        return response ?: throw (lastException ?: IOException("Request failed after $MAX_RETRIES attempts"))
    }
}
