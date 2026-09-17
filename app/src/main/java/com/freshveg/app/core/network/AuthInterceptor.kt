package com.freshveg.app.core.network

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import com.freshveg.app.core.datastore.SessionManager

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = runBlocking { sessionManager.accessToken.firstOrNull() }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())
        return response
    }
}
