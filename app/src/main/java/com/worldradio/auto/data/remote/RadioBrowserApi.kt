package com.worldradio.auto.data.remote

import com.worldradio.auto.data.model.CountryDto
import com.worldradio.auto.data.model.StationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    @GET("stations/bycountrycodeexact/{code}")
    suspend fun getStationsByCountry(
        @Path("code") countryCode: String,
        @Query("limit") limit: Int = 500,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<StationDto>

    @GET("stations/topvote/{limit}")
    suspend fun getTopVotedStations(
        @Path("limit") limit: Int = 60,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<StationDto>

    @GET("stations/topclick/{limit}")
    suspend fun getTopClickedStations(
        @Path("limit") limit: Int = 60,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<StationDto>

    @GET("countries")
    suspend fun getCountries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true
    ): List<CountryDto>

    @GET("stations/byname/{name}")
    suspend fun searchStationsByName(
        @Path("name") name: String,
        @Query("limit") limit: Int = 50,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<StationDto>
}
