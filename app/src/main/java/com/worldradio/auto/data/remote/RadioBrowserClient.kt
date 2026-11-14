package com.worldradio.auto.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RadioBrowserClient {
    // Distributed public mirrors for maximum uptime and global coverage
    val PUBLIC_MIRRORS = listOf(
        "de1.api.radio-browser.info",
        "at1.api.radio-browser.info",
        "nl1.api.radio-browser.info",
        "all.api.radio-browser.info"
    )

    private const val BASE_URL = "https://de1.api.radio-browser.info/json/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Interceptor with automatic failover across all public Radio Browser mirrors.
     * CRITICAL: Only intercepts requests aimed at radio-browser.info API!
     * Never interferes with actual audio stream URLs.
     */
    private val failoverInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // If not a Radio Browser API request, pass through directly!
        if (!originalRequest.url.host.contains("radio-browser.info")) {
            return@Interceptor chain.proceed(originalRequest)
        }

        var lastException: IOException? = null
        var response: Response? = null

        for (host in PUBLIC_MIRRORS) {
            val newUrl = originalRequest.url.newBuilder()
                .host(host)
                .build()
            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .header("User-Agent", "WorldRadioAuto/1.0 (AndroidAutoRadio; OpenSource)")
                .build()

            try {
                response = chain.proceed(newRequest)
                if (response.isSuccessful) {
                    return@Interceptor response
                } else if (response.code in 500..599) {
                    response.close()
                    continue
                } else {
                    return@Interceptor response
                }
            } catch (e: IOException) {
                lastException = e
            }
        }

        if (response != null) return@Interceptor response
        throw lastException ?: IOException("All public radio servers unavailable")
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .addInterceptor(failoverInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    val api: RadioBrowserApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadioBrowserApi::class.java)
    }
}
