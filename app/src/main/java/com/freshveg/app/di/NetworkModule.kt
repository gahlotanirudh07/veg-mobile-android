package com.freshveg.app.di

import com.freshveg.app.core.network.ApiConfig
import com.freshveg.app.core.network.AuthInterceptor
import com.freshveg.app.core.network.RetryInterceptor
import com.freshveg.app.core.network.VegApiService
import com.google.gson.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        val doubleDeserializer = JsonDeserializer<Double> { json, _, _ ->
            try {
                if (json == null || json.isJsonNull) 0.0
                else if (json.isJsonPrimitive) json.asString.toDoubleOrNull() ?: 0.0
                else 0.0
            } catch (e: Exception) {
                0.0
            }
        }

        val intDeserializer = JsonDeserializer<Int> { json, _, _ ->
            try {
                if (json == null || json.isJsonNull) 0
                else if (json.isJsonPrimitive) json.asString.toIntOrNull() ?: json.asString.toDoubleOrNull()?.toInt() ?: 0
                else 0
            } catch (e: Exception) {
                0
            }
        }

        return GsonBuilder()
            .registerTypeAdapter(Double::class.java, doubleDeserializer)
            .registerTypeAdapter(Double::class.javaObjectType, doubleDeserializer)
            .registerTypeAdapter(Int::class.java, intDeserializer)
            .registerTypeAdapter(Int::class.javaObjectType, intDeserializer)
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideVegApiService(retrofit: Retrofit): VegApiService {
        return retrofit.create(VegApiService::class.java)
    }
}
