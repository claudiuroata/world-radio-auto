package com.worldradio.auto.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.worldradio.auto.data.local.RadioDatabase
import com.worldradio.auto.data.local.StationDao
import com.worldradio.auto.data.model.CountryDto
import com.worldradio.auto.data.model.CuratedStations
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.data.remote.FakeRadioBrowserApi
import com.worldradio.auto.data.remote.FakeRadioBrowserApi.Companion.dto
import com.worldradio.auto.util.UserLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioRepositoryTest {

    private lateinit var db: RadioDatabase
    private lateinit var dao: StationDao
    private lateinit var api: FakeRadioBrowserApi
    private lateinit var repository: RadioRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RadioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.stationDao()
        api = FakeRadioBrowserApi()
        repository = RadioRepository(api, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getPopularStations_sortsByVotesDesc_enrichesFavorites_andExcludesHidden() = runBlocking {
        val favStation = Station(id = "pop_low", name = "Pop Low", streamUrl = "https://s/1")
        val hiddenStation = Station(id = "pop_mid", name = "Pop Hidden", streamUrl = "https://s/2")
        repository.toggleFavorite(favStation) // inserts favorite=true
        assertTrue(repository.toggleHidden(hiddenStation)) // inserts hidden=true

        api.topVoted = listOf(
            dto(id = "pop_low", name = "Pop Low", votes = 100),
            dto(id = "pop_mid", name = "Pop Hidden", votes = 500),
            dto(id = "pop_high", name = "Pop High", votes = 900)
        )

        val result = repository.getPopularStations()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(listOf("pop_high", "pop_low"), list.map { it.id })
        assertTrue(list.last().isFavorite)
    }

    @Test
    fun getPopularStations_returnsFailureWhenApiThrows() = runBlocking {
        api.shouldFailTopVoted = true
        val result = repository.getPopularStations()
        assertTrue(result.isFailure)
    }

    @Test
    fun getLocalStations_normalizesBlankToRO_sortsByClickThenVotes_andExcludesHidden() = runBlocking {
        api.stationsByCountry["RO"] = listOf(
            dto(id = "s1", name = "Clicks 10 Votes 500", clickCount = 10, votes = 500),
            dto(id = "s2", name = "Clicks 50 Votes 100", clickCount = 50, votes = 100),
            dto(id = "s3", name = "Clicks 50 Votes 300", clickCount = 50, votes = 300),
            dto(id = "s_hid", name = "Hidden Local", clickCount = 999, votes = 999)
        )
        repository.toggleHidden(Station(id = "s_hid", name = "Hidden Local", streamUrl = "https://s/hid"))

        val blankResult = repository.getLocalStations("   ")
        assertTrue(blankResult.isSuccess)
        assertEquals("RO", api.recordedCountryCalls.last())
        assertEquals(listOf("s3", "s2", "s1"), blankResult.getOrThrow().map { it.id })

        api.stationsByCountry["FR"] = listOf(dto(id = "fr_1", name = "France Inter", country = "France", countryCode = "FR"))
        val lowerResult = repository.getLocalStations(" fr ")
        assertTrue(lowerResult.isSuccess)
        assertEquals("FR", api.recordedCountryCalls.last())
        assertEquals(listOf("fr_1"), lowerResult.getOrThrow().map { it.id })
    }

    @Test
    fun getLocalStationsAroundLocation_filtersByRegionOrCity_mergesCurated_sortsAZ_andExcludesHidden() = runBlocking {
        api.stationsByCountry["RO"] = listOf(
            dto(id = "api_cluj", name = "Radio Cluj API", state = "Cluj", votes = 50),
            dto(id = "api_iasi", name = "Radio Iasi API", state = "Iasi", votes = 500),
            dto(id = "api_other", name = "Zero Votes", state = "Timis", votes = 0)
        )
        // Hide one regional curated station and verify it does NOT appear in local results
        val hiddenRegional = CuratedStations.getRegionalStationsFor("Cluj", "Cluj-Napoca").first()
        assertTrue(repository.toggleHidden(hiddenRegional))

        val location = UserLocation(
            city = "Cluj-Napoca",
            region = "Cluj",
            countryCode = "RO",
            country = "Romania"
        )
        val result = repository.getLocalStationsAroundLocation(location)
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()

        assertTrue(list.any { it.id == "api_cluj" })
        assertFalse(list.any { it.id == "api_iasi" })
        assertFalse(list.any { it.id == hiddenRegional.id })
        assertTrue(list.any { it.id == "curated_cluj_2" })
        val names = list.map { it.name.lowercase() }
        assertEquals(names.sorted(), names)

        // When region has no API matches & no regional curated stations, fallback excludes hidden starter stations too
        val hiddenStarter = CuratedStations.ROMANIA_STARTER_STATIONS.first()
        assertTrue(repository.toggleHidden(hiddenStarter))
        val fallbackResult = repository.getLocalStationsAroundLocation(
            UserLocation(city = "UnknownCity", region = "UnknownRegion", countryCode = "RO", country = "Romania")
        )
        assertTrue(fallbackResult.isSuccess)
        val fallbackList = fallbackResult.getOrThrow()
        assertFalse(fallbackList.any { it.id == hiddenStarter.id })
        assertTrue(fallbackList.any { it.id == "curated_ro_2" })
    }

    @Test
    fun getCountries_filtersStationCountOver10_uppercasesCode_andSortsAZ() = runBlocking {
        api.countries = listOf(
            CountryDto(name = "Tiny Land", code = "tl", stationCount = 10),
            CountryDto(name = "Blank Code", code = " ", stationCount = 100),
            CountryDto(name = "Romania", code = "ro", stationCount = 250),
            CountryDto(name = "Austria", code = "at", stationCount = 300)
        )

        val result = repository.getCountries()
        assertTrue(result.isSuccess)
        val countries = result.getOrThrow()
        assertEquals(listOf("Austria", "Romania"), countries.map { it.name })
        assertEquals(listOf("AT", "RO"), countries.map { it.code })
    }

    @Test
    fun getStationsByCountry_andSearchStations_excludeHiddenStationsAndHandleErrors() = runBlocking {
        val hiddenStation = Station(id = "de_hid", name = "Bayern Hidden", streamUrl = "https://s/hid")
        repository.toggleHidden(hiddenStation)

        api.stationsByCountry["DE"] = listOf(
            dto(id = "de_1", name = "SWR3", countryCode = "DE", votes = 100),
            dto(id = "de_hid", name = "Bayern Hidden", countryCode = "DE", votes = 900),
            dto(id = "de_2", name = "Antenne Bayern", countryCode = "DE", votes = 500)
        )
        val deResult = repository.getStationsByCountry("de")
        assertTrue(deResult.isSuccess)
        assertEquals(listOf("de_2", "de_1"), deResult.getOrThrow().map { it.id })

        api.defaultSearchResults = listOf(
            dto(id = "de_1", name = "Zeta Jazz"),
            dto(id = "de_hid", name = "Bayern Hidden"),
            dto(id = "s_alpha", name = "Alpha Jazz")
        )
        val searchResult = repository.searchStations("jazz")
        assertTrue(searchResult.isSuccess)
        assertEquals(listOf("s_alpha", "de_1"), searchResult.getOrThrow().map { it.id })

        api.shouldFailByCountry = true
        assertTrue(repository.getStationsByCountry("de").isFailure)

        api.shouldFailSearch = true
        assertTrue(repository.searchStations("jazz").isFailure)
    }

    @Test
    fun toggleHidden_andToggleFavorite_preserveOtherFlagAndLastPlayedTimestamp() = runBlocking {
        val station = Station(
            id = "mix_1",
            name = "Mix Station",
            streamUrl = "https://stream.mix/1",
            country = "Romania",
            countryCode = "RO"
        )

        // Play -> favorite -> hide -> unhide -> recordPlayed while hidden
        repository.recordPlayed(station)
        val afterPlayed = repository.getStationById("mix_1")
        assertNotNull(afterPlayed)
        assertTrue(afterPlayed!!.lastPlayedTimestamp > 0L)
        assertFalse(afterPlayed.isFavorite)
        assertFalse(afterPlayed.isHidden)

        repository.toggleFavorite(afterPlayed)
        val afterFav = repository.getStationById("mix_1")!!
        assertTrue(afterFav.isFavorite)
        assertFalse(afterFav.isHidden)

        // Hide station
        val nowHidden = repository.toggleHidden(afterFav)
        assertTrue(nowHidden)
        val afterHide = repository.getStationById("mix_1")!!
        assertTrue(afterHide.isHidden)
        assertTrue(afterHide.isFavorite)
        assertEquals(afterPlayed.lastPlayedTimestamp, afterHide.lastPlayedTimestamp)

        // While hidden: excluded from FavoritesSync, RecentSync, Local, Popular, World, Search
        assertTrue(repository.getFavoriteStationsSync().isEmpty())
        assertTrue(repository.getRecentStationsSync().isEmpty())
        assertEquals(listOf("mix_1"), repository.getHiddenStationsSync().map { it.id })
        assertEquals(setOf("mix_1"), repository.getHiddenStationIdsSync())

        // Playing from Hidden tab calls recordPlayed -> must stay hidden and stay favorite, stay out of Recent!
        repository.recordPlayed(afterHide)
        val afterPlayedWhileHidden = repository.getStationById("mix_1")!!
        assertTrue(afterPlayedWhileHidden.isHidden)
        assertTrue(afterPlayedWhileHidden.isFavorite)
        assertTrue(repository.getRecentStationsSync().isEmpty())
        assertEquals(listOf("mix_1"), repository.getHiddenStations().first().map { it.id })

        // Unhide -> should return to Favorites & Recents with preserved favorite state
        val afterUnhideFlag = repository.toggleHidden(afterPlayedWhileHidden)
        assertFalse(afterUnhideFlag)
        assertEquals(listOf("mix_1"), repository.getFavoriteStationsSync().map { it.id })
        assertEquals(listOf("mix_1"), repository.getRecentStationsSync().map { it.id })
        assertTrue(repository.getHiddenStationsSync().isEmpty())
    }
}
