package com.worldradio.auto.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.worldradio.auto.data.model.Station

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationId: String,
    val name: String,
    val streamUrl: String,
    val favicon: String?,
    val country: String?,
    val countryCode: String?,
    val state: String?,
    val language: String?,
    val tags: String?,
    val votes: Int,
    val codec: String?,
    val bitrate: Int,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
) {
    fun toDomain(): Station {
        return Station(
            id = stationId,
            name = name,
            streamUrl = streamUrl,
            favicon = favicon,
            country = country,
            countryCode = countryCode,
            state = state,
            language = language,
            tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            votes = votes,
            codec = codec,
            bitrate = bitrate,
            isFavorite = isFavorite,
            isHidden = isHidden,
            lastPlayedTimestamp = lastPlayedTimestamp
        )
    }

    companion object {
        fun fromDomain(
            station: Station,
            isFavorite: Boolean = station.isFavorite,
            isHidden: Boolean = station.isHidden,
            lastPlayed: Long = station.lastPlayedTimestamp
        ): StationEntity {
            return StationEntity(
                stationId = station.id,
                name = station.name,
                streamUrl = station.streamUrl,
                favicon = station.favicon,
                country = station.country,
                countryCode = station.countryCode,
                state = station.state,
                language = station.language,
                tags = station.tags.joinToString(","),
                votes = station.votes,
                codec = station.codec,
                bitrate = station.bitrate,
                isFavorite = isFavorite,
                isHidden = isHidden,
                lastPlayedTimestamp = lastPlayed
            )
        }
    }
}
