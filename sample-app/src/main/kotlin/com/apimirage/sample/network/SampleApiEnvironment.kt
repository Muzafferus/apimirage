package com.apimirage.sample.network

import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.retrofit.ApiMirageInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

public object SampleApiEnvironment {
    private const val BASE_URL: String = "https://apimirage.local/"

    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    public fun createService(
        config: ApiMirageConfig = ApiMirage.currentConfig(),
    ): SampleApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor(config))
            .addInterceptor(ApiMirageInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(SampleApiService::class.java)
    }

    private fun createLoggingInterceptor(config: ApiMirageConfig): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (config.diagnostics == ApiMirageDiagnostics.LOGS) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
}

