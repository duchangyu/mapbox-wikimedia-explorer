package com.example.awsome_car.di

import android.content.Context
import com.example.awsome_car.data.remote.WikimediaApiService
import com.example.awsome_car.data.repository.WikimediaRepository
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private const val CACHE_SIZE_MB = 10L
    private const val CACHE_DIR_NAME = "http_cache"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private var _repository: WikimediaRepository? = null
    val repository: WikimediaRepository
        get() = _repository ?: throw IllegalStateException(
            "ServiceLocator not initialized. Call init(context) in Application.onCreate()"
        )

    fun init(context: Context) {
        if (_repository != null) return

        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val cache = Cache(cacheDir, CACHE_SIZE_MB * 1024 * 1024)

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36 AwesomeCarApp/1.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://commons.wikimedia.org/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val apiService = retrofit.create(WikimediaApiService::class.java)
        _repository = WikimediaRepository(apiService)
    }
}
