package com.worldradio.auto.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.worldradio.auto.ui.screens.countryCodeToEmoji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UtilsAndModelsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CountryDetector.clearUserSelectedLocation(context)
    }

    @Test
    fun countryCodeToEmoji_mapsValidCodesAndFallsBackForInvalid() {
        assertEquals("🇷🇴", countryCodeToEmoji("RO"))
        assertEquals("🇺🇸", countryCodeToEmoji("us"))
        assertEquals("🇯🇵", countryCodeToEmoji("Jp"))
        assertEquals("🌍", countryCodeToEmoji(""))
        assertEquals("🌍", countryCodeToEmoji("A"))
        assertEquals("🌍", countryCodeToEmoji("USA"))
    }

    @Test
    fun userLocation_displayAreaName_formatsAllCombinations() {
        assertEquals(
            "Cluj-Napoca, Cluj",
            UserLocation("Cluj-Napoca", "Cluj", "RO", "Romania").displayAreaName
        )
        assertEquals(
            "Bucharest",
            UserLocation("Bucharest", "Bucharest", "RO", "Romania").displayAreaName
        )
        assertEquals(
            "Cluj-Napoca",
            UserLocation("Cluj-Napoca", "", "RO", "Romania").displayAreaName
        )
        assertEquals(
            "Transylvania",
            UserLocation("", "Transylvania", "RO", "Romania").displayAreaName
        )
        assertEquals(
            "Romania",
            UserLocation("", "", "RO", "Romania").displayAreaName
        )
    }

    @Test
    fun countryDetector_manualOverrideTakesPrecedenceAndCanBeCleared() {
        assertNull(CountryDetector.getUserSelectedLocation(context))

        CountryDetector.setUserSelectedLocation(context, "  Sibiu ", " Sibiu ")
        val manual = CountryDetector.getUserSelectedLocation(context)
        assertNotNull(manual)
        assertEquals("Sibiu", manual!!.city)
        assertEquals("Sibiu", manual.region)
        assertFalse(manual.isAutoDetected)

        val detected = CountryDetector.detectLocation(context)
        assertEquals("Sibiu", detected.city)
        assertFalse(detected.isAutoDetected)

        CountryDetector.clearUserSelectedLocation(context)
        assertNull(CountryDetector.getUserSelectedLocation(context))
    }

    @Test
    fun countryDetector_timezoneFallbackResolvesExpectedCountries() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"))
            assertEquals("RO", CountryDetector.detectCountryCode(context))
            assertTrue(CountryDetector.detectLocation(context).isAutoDetected)

            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            assertEquals("DE", CountryDetector.detectCountryCode(context))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
