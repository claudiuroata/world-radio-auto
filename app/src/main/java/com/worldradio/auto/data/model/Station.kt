package com.worldradio.auto.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StationDto(
    @Json(name = "stationuuid") val stationuuid: String,
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String,
    @Json(name = "url_resolved") val urlResolved: String?,
    @Json(name = "homepage") val homepage: String?,
    @Json(name = "favicon") val favicon: String?,
    @Json(name = "tags") val tags: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "countrycode") val countrycode: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "votes") val votes: Int?,
    @Json(name = "clickcount") val clickcount: Int? = 0,
    @Json(name = "clicktrend") val clicktrend: Int? = 0,
    @Json(name = "codec") val codec: String?,
    @Json(name = "bitrate") val bitrate: Int?
)

data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val favicon: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    val votes: Int = 0,
    val clickCount: Int = 0,
    val codec: String? = null,
    val bitrate: Int = 0,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CountryDto(
    @Json(name = "name") val name: String,
    @Json(name = "iso_3166_1") val code: String,
    @Json(name = "stationcount") val stationCount: Int
)

data class Country(
    val name: String,
    val code: String,
    val stationCount: Int
)
