package com.worldradio.auto.ui.viewmodel

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.worldradio.auto.data.local.RadioDatabase
import com.worldradio.auto.data.model.Country
import com.worldradio.auto.data.model.CountryDto
import com.worldradio.auto.data.model.CuratedStations
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.data.remote.FakeRadioBrowserApi
import com.worldradio.auto.data.remote.FakeRadioBrowserApi.Companion.dto
import com.worldradio.auto.data.repository.RadioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var db: RadioDatabase
    private lateinit var api: FakeRadioBrowserApi
    private lateinit var repository: RadioRepository
    private lateinit var viewModel: RadioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, RadioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeRadioBrowserApi()
        repository = RadioRepository(api, db.stationDao())
        viewModel = RadioViewModel(app, repository, autoConnect = false)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun awaitUntil(timeoutMs: Long = 3000L, predicate: () -> Boolean) = runBlocking {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            delay(15)
        }
        assertTrue("Condition was not met within ${timeoutMs}ms", predicate())
    }

    @Test
    fun loadLocalStations_andToggleHidden_reactivelyRemovesAndRestoresAcrossQueuesAndTabs() {
        api.stationsByCountry["RO"] = listOf(
            dto(id = "loc_1", name = "Local One", clickCount = 100),
            dto(id = "loc_2", name = "Local Two", clickCount = 80),
            dto(id = "loc_3", name = "Local Three", clickCount = 60)
        )

        viewModel.loadLocalStations("RO")
        awaitUntil { viewModel.localStations.value.size == 3 }
        assertEquals(listOf("loc_1", "loc_2", "loc_3"), viewModel.localStations.value.map { it.id })

        val target = viewModel.localStations.value[1] // loc_2
        // Mark favorite first so we also test hiding removes from Favorites and unhiding restores it
        viewModel.toggleFavorite(target)
        awaitUntil { viewModel.favoriteStations.value.any { it.id == "loc_2" } }

        // Hide loc_2
        viewModel.toggleHidden(target)
        awaitUntil {
            viewModel.hiddenStations.value.any { it.id == "loc_2" } &&
                viewModel.localStations.value.none { it.id == "loc_2" } &&
                viewModel.favoriteStations.value.none { it.id == "loc_2" }
        }
        assertEquals(listOf("loc_1", "loc_3"), viewModel.localStations.value.map { it.id })
        assertTrue(viewModel.hiddenStations.value.first().isHidden)

        // Unhide loc_2 -> restores to Local AND Favorites immediately without network refetch
        val hiddenTarget = viewModel.hiddenStations.value.first()
        viewModel.toggleHidden(hiddenTarget)
        awaitUntil {
            viewModel.hiddenStations.value.isEmpty() &&
                viewModel.localStations.value.any { it.id == "loc_2" } &&
                viewModel.favoriteStations.value.any { it.id == "loc_2" }
        }
        assertEquals(listOf("loc_1", "loc_2", "loc_3"), viewModel.localStations.value.map { it.id })
    }

    @Test
    fun selectCountry_andSearch_reactivelyFilterHiddenStationsAndAllowClear() {
        api.stationsByCountry["JP"] = listOf(
            dto(id = "jp_1", name = "Tokyo FM", country = "Japan", countryCode = "JP", votes = 200),
            dto(id = "jp_2", name = "Osaka Jazz", country = "Japan", countryCode = "JP", votes = 100)
        )
        api.defaultSearchResults = listOf(
            dto(id = "jp_1", name = "Tokyo FM"),
            dto(id = "jp_2", name = "Osaka Jazz")
        )

        val japan = Country("Japan", "JP", 500)
        viewModel.selectCountry(japan)
        awaitUntil { viewModel.countryStations.value.size == 2 }

        viewModel.onSearchQueryChanged("Jazz")
        mainDispatcher.scheduler.advanceTimeBy(350)
        mainDispatcher.scheduler.runCurrent()
        awaitUntil(timeoutMs = 3000L) { viewModel.searchResults.value.size == 2 }

        // Hide jp_2 -> excluded from BOTH countryStations and searchResults reactively
        viewModel.toggleHidden(Station(id = "jp_2", name = "Osaka Jazz", streamUrl = "https://stream/jp_2"))
        awaitUntil {
            viewModel.countryStations.value.map { it.id } == listOf("jp_1") &&
                viewModel.searchResults.value.map { it.id } == listOf("jp_1")
        }

        // Clearing selected country resets selection and countryStations
        viewModel.clearSelectedCountry()
        assertNull(viewModel.selectedCountry.value)
        assertTrue(viewModel.countryStations.value.isEmpty())

        // Clearing search query empties results immediately
        viewModel.onSearchQueryChanged("")
        awaitUntil { viewModel.searchResults.value.isEmpty() }
    }

    @Test
    fun playStation_blocksHiddenUnlessAllowHiddenIsTrue_andUpdatesCurrentStationFlags() = runBlocking {
        val hiddenStation = Station(
            id = "hid_play",
            name = "Hidden Beats",
            streamUrl = "https://stream/hid_play.mp3"
        )
        repository.toggleHidden(hiddenStation)
        awaitUntil { viewModel.hiddenStations.value.any { it.id == "hid_play" } }

        // Attempt to play from a normal queue (allowHidden = false) must be ignored
        viewModel.playStation(hiddenStation, listOf(hiddenStation), allowHidden = false)
        assertNull(viewModel.currentStation.value)

        // Playing explicitly from Hidden tab (allowHidden = true) updates currentStation & keeps it hidden
        viewModel.playStation(hiddenStation.copy(isHidden = true), listOf(hiddenStation), allowHidden = true)
        awaitUntil { viewModel.currentStation.value?.id == "hid_play" }
        assertTrue(viewModel.currentStation.value!!.isHidden)
        assertTrue(repository.getRecentStationsSync().isEmpty())

        // Toggling favorite reflects live on currentStation while hidden, and unhiding updates currentStation
        viewModel.toggleFavorite(viewModel.currentStation.value!!)
        awaitUntil { viewModel.currentStation.value?.isFavorite == true }
        assertTrue(viewModel.currentStation.value!!.isHidden)

        viewModel.toggleHidden(viewModel.currentStation.value!!)
        awaitUntil { viewModel.currentStation.value?.isHidden == false }
    }

    @Test
    fun loadLocalStations_fallsBackToCuratedOnFailureAndStillExcludesHiddenCuratedStations() = runBlocking {
        val curatedFirst = CuratedStations.ROMANIA_STARTER_STATIONS.first()
        repository.toggleHidden(curatedFirst)
        api.shouldFailByCountry = true

        viewModel.loadLocalStations("RO")
        awaitUntil { !viewModel.isLoading.value && viewModel.localStations.value.isNotEmpty() }

        assertFalse(viewModel.localStations.value.any { it.id == curatedFirst.id })
        assertTrue(viewModel.localStations.value.any { it.id == "curated_ro_2" })
    }

    @Test
    fun loadCountries_filtersAndLoads_andFallsBackWhenApiFails() {
        api.countries = listOf(CountryDto("Germany", "DE", 1200))
        val successVm = RadioViewModel(app, repository, autoConnect = false)
        successVm.loadCountries()
        awaitUntil { successVm.countries.value.any { it.code == "DE" } }

        api.shouldFailCountries = true
        val fallbackVm = RadioViewModel(app, repository, autoConnect = false)
        fallbackVm.loadCountries()
        awaitUntil { fallbackVm.countries.value.isNotEmpty() }
    }
}
