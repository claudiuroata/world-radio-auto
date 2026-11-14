package com.worldradio.auto.util

import android.content.Context
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class UserLocation(
    val city: String,
    val region: String,
    val countryCode: String,
    val country: String,
    val isAutoDetected: Boolean = true
) {
    val displayAreaName: String
        get() = when {
            city.isNotBlank() && region.isNotBlank() && !city.equals(region, ignoreCase = true) -> "$city, $region"
            city.isNotBlank() -> city
            region.isNotBlank() -> region
            else -> country
        }
}

object CountryDetector {

    private const val PREFS_NAME = "radio_country_prefs"
    private const val KEY_MANUAL_CITY = "manual_city"
    private const val KEY_MANUAL_REGION = "manual_region"
    private const val KEY_CACHED_CITY = "cached_city"
    private const val KEY_CACHED_REGION = "cached_region"
    private const val KEY_CACHED_COUNTRY_CODE = "cached_country_code"
    private const val KEY_CACHED_COUNTRY = "cached_country"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    fun getUserSelectedLocation(context: Context): UserLocation? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_MANUAL_CITY, null)
        val region = prefs.getString(KEY_MANUAL_REGION, null)
        val countryCode = prefs.getString(KEY_CACHED_COUNTRY_CODE, "RO") ?: "RO"
        val country = prefs.getString(KEY_CACHED_COUNTRY, "Romania") ?: "Romania"
        return if (!city.isNullOrBlank() || !region.isNullOrBlank()) {
            UserLocation(
                city = city ?: "",
                region = region ?: "",
                countryCode = countryCode,
                country = country,
                isAutoDetected = false
            )
        } else {
            null
        }
    }

    fun setUserSelectedLocation(context: Context, city: String, region: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_MANUAL_CITY, city.trim())
            .putString(KEY_MANUAL_REGION, region.trim())
            .apply()
    }

    fun clearUserSelectedLocation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_MANUAL_CITY)
            .remove(KEY_MANUAL_REGION)
            .apply()
    }

    /**
     * Resolves the current user location:
     * 1. Manual selection if set
     * 2. Cached IP geo-location from preferences
     * 3. Fallback based on timezone / cellular carrier (e.g. Europe/Bucharest -> Bucharest / Romania)
     */
    fun detectLocation(context: Context): UserLocation {
        // 1. Check user manual override
        getUserSelectedLocation(context)?.let { return it }

        // 2. Check cached location
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedCity = prefs.getString(KEY_CACHED_CITY, null)
        val cachedRegion = prefs.getString(KEY_CACHED_REGION, null)
        val cachedCode = prefs.getString(KEY_CACHED_COUNTRY_CODE, null)
        val cachedCountry = prefs.getString(KEY_CACHED_COUNTRY, null)

        if (!cachedCity.isNullOrBlank() && !cachedRegion.isNullOrBlank()) {
            return UserLocation(
                city = cachedCity,
                region = cachedRegion,
                countryCode = cachedCode ?: "RO",
                country = cachedCountry ?: "Romania",
                isAutoDetected = true
            )
        }

        // 3. Fallback
        val countryCode = detectCountryCode(context)
        return if (countryCode.equals("RO", ignoreCase = true)) {
            UserLocation(
                city = "Bucharest",
                region = "Bucharest",
                countryCode = "RO",
                country = "Romania",
                isAutoDetected = true
            )
        } else {
            UserLocation(
                city = "",
                region = "",
                countryCode = countryCode,
                country = countryCode,
                isAutoDetected = true
            )
        }
    }

    /**
     * Accurately detects country code from SIM, cellular, or timezone
     */
    fun detectCountryCode(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val networkCountry = tm?.networkCountryIso?.trim()?.uppercase()
            if (!networkCountry.isNullOrBlank() && networkCountry.length == 2) {
                return networkCountry
            }

            val simCountry = tm?.simCountryIso?.trim()?.uppercase()
            if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                return simCountry
            }
        } catch (_: Exception) { }

        val tzId = TimeZone.getDefault().id
        when {
            tzId.contains("Bucharest", ignoreCase = true) -> return "RO"
            tzId.contains("Chisinau", ignoreCase = true) -> return "MD"
            tzId.contains("London", ignoreCase = true) -> return "GB"
            tzId.contains("Berlin", ignoreCase = true) -> return "DE"
            tzId.contains("Paris", ignoreCase = true) -> return "FR"
            tzId.contains("Rome", ignoreCase = true) -> return "IT"
            tzId.contains("Madrid", ignoreCase = true) -> return "ES"
            tzId.contains("Vienna", ignoreCase = true) -> return "AT"
            tzId.contains("Warsaw", ignoreCase = true) -> return "PL"
            tzId.contains("Athens", ignoreCase = true) -> return "GR"
            tzId.contains("Budapest", ignoreCase = true) -> return "HU"
            tzId.contains("Sofia", ignoreCase = true) -> return "BG"
            tzId.contains("New_York", ignoreCase = true) || tzId.contains("Los_Angeles", ignoreCase = true) ||
            tzId.contains("Chicago", ignoreCase = true) -> return "US"
        }

        val localeCountry = Locale.getDefault().country?.trim()?.uppercase()
        if (!localeCountry.isNullOrBlank() && localeCountry.length == 2) {
            return localeCountry
        }

        val offsetHours = TimeZone.getDefault().rawOffset / (1000 * 60 * 60)
        if (offsetHours in 2..3) {
            return "RO"
        }

        return "RO"
    }

    /**
     * Background IP lookup detecting specific City & Region/State.
     * Caches in preferences for instant offline startup.
     */
    suspend fun fetchIpLocation(context: Context): UserLocation? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://ip-api.com/json")
                .header("User-Agent", "WorldRadioAuto/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            if (json.optString("status") == "success") {
                val city = json.optString("city", "").trim()
                val regionName = json.optString("regionName", "").trim()
                val countryCode = json.optString("countryCode", "RO").trim().uppercase()
                val country = json.optString("country", "Romania").trim()

                // Cache in preferences
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_CACHED_CITY, city)
                    .putString(KEY_CACHED_REGION, regionName)
                    .putString(KEY_CACHED_COUNTRY_CODE, countryCode)
                    .putString(KEY_CACHED_COUNTRY, country)
                    .apply()

                UserLocation(
                    city = city,
                    region = regionName,
                    countryCode = countryCode,
                    country = country,
                    isAutoDetected = true
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
