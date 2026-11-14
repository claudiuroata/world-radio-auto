package com.worldradio.auto.data.remote

import com.worldradio.auto.data.model.CountryDto
import com.worldradio.auto.data.model.StationDto
import java.io.IOException

/**
 * In-memory fake implementation of [RadioBrowserApi] for deterministic unit testing
 * of repository and ViewModel behavior without hitting real network endpoints.
 */
class FakeRadioBrowserApi : RadioBrowserApi {
    var topVoted: List<StationDto> = emptyList()
    var topClicked: List<StationDto> = emptyList()
    var stationsByCountry: MutableMap<String, List<StationDto>> = mutableMapOf()
    var countries: List<CountryDto> = emptyList()
    var searchResultsByQuery: MutableMap<String, List<StationDto>> = mutableMapOf()
    var defaultSearchResults: List<StationDto> = emptyList()

    var shouldFailByCountry: Boolean = false
    var shouldFailTopVoted: Boolean = false
    var shouldFailTopClicked: Boolean = false
    var shouldFailCountries: Boolean = false
    var shouldFailSearch: Boolean = false

    val recordedCountryCalls = mutableListOf<String>()
    val recordedSearchQueries = mutableListOf<String>()

    override suspend fun getStationsByCountry(
        countryCode: String,
        limit: Int,
        order: String,
        reverse: Boolean,
        hideBroken: Boolean
    ): List<StationDto> {
        recordedCountryCalls.add(countryCode)
        if (shouldFailByCountry) throw IOException("Simulated network error for country=$countryCode")
        return (stationsByCountry[countryCode] ?: stationsByCountry[countryCode.uppercase()] ?: emptyList())
            .take(limit)
    }

    override suspend fun getTopVotedStations(limit: Int, hideBroken: Boolean): List<StationDto> {
        if (shouldFailTopVoted) throw IOException("Simulated top voted failure")
        return topVoted.take(limit)
    }

    override suspend fun getTopClickedStations(limit: Int, hideBroken: Boolean): List<StationDto> {
        if (shouldFailTopClicked) throw IOException("Simulated top clicked failure")
        return topClicked.take(limit)
    }

    override suspend fun getCountries(order: String, reverse: Boolean): List<CountryDto> {
        if (shouldFailCountries) throw IOException("Simulated countries failure")
        return countries
    }

    override suspend fun searchStationsByName(
        name: String,
        limit: Int,
        hideBroken: Boolean
    ): List<StationDto> {
        recordedSearchQueries.add(name)
        if (shouldFailSearch) throw IOException("Simulated search failure for '$name'")
        return (searchResultsByQuery[name] ?: defaultSearchResults).take(limit)
    }

    companion object {
        fun dto(
            id: String,
            name: String = "Station $id",
            url: String = "https://stream.example.com/$id.mp3",
            urlResolved: String? = null,
            favicon: String? = "https://img.example.com/$id.png",
            tags: String? = "pop,news",
            country: String? = "Romania",
            countryCode: String? = "RO",
            state: String? = "Bucharest",
            language: String? = "romanian",
            votes: Int = 100,
            clickCount: Int = 50,
            codec: String? = "MP3",
            bitrate: Int = 128
        ): StationDto = StationDto(
            stationuuid = id,
            name = name,
            url = url,
            urlResolved = urlResolved,
            homepage = null,
            favicon = favicon,
            tags = tags,
            country = country,
            countrycode = countryCode,
            state = state,
            language = language,
            votes = votes,
            clickcount = clickCount,
            clicktrend = 0,
            codec = codec,
            bitrate = bitrate
        )
    }
}
