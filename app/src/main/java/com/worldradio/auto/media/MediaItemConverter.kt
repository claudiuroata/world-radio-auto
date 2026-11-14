package com.worldradio.auto.media

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.worldradio.auto.data.model.Country
import com.worldradio.auto.data.model.Station

object MediaItemConverter {

    const val ROOT_ID = "[ROOT]"
    const val TAB_RECENT = "[TAB_RECENT]"
    const val TAB_FAVORITES = "[TAB_FAVORITES]"
    const val TAB_LOCAL = "[TAB_LOCAL]"
    const val TAB_WORLD = "[TAB_WORLD]"
    const val TAB_POPULAR = "[TAB_POPULAR]"
    const val TAB_SUGGESTED = "[TAB_SUGGESTED]"
    const val TAB_HIDDEN = "[TAB_HIDDEN]"
    const val PREFIX_COUNTRY = "COUNTRY_"

    // Android Auto content styling keys
    const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
    const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
    const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
    const val CONTENT_STYLE_LIST = 1
    const val CONTENT_STYLE_GRID = 2

    const val MEDIA_ID_DELIMITER = "||"

    fun buildMediaId(parentId: String?, stationId: String): String {
        return if (!parentId.isNullOrBlank()) {
            "$parentId$MEDIA_ID_DELIMITER$stationId"
        } else {
            stationId
        }
    }

    fun parseMediaId(mediaId: String): Pair<String?, String> {
        return if (mediaId.contains(MEDIA_ID_DELIMITER)) {
            val parent = mediaId.substringBefore(MEDIA_ID_DELIMITER)
            val id = mediaId.substringAfter(MEDIA_ID_DELIMITER)
            Pair(parent, id)
        } else {
            Pair(null, mediaId)
        }
    }

    fun buildRootItem(): MediaItem {
        val rootExtras = Bundle().apply {
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
        }

        return MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("World Radio Auto")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                    .setExtras(rootExtras)
                    .build()
            )
            .build()
    }

    fun buildSuggestedRootItem(): MediaItem {
        val rootExtras = Bundle().apply {
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
            putBoolean("android.service.media.extra.SUGGESTED", true)
        }

        return MediaItem.Builder()
            .setMediaId(TAB_SUGGESTED)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("For You")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                    .setExtras(rootExtras)
                    .build()
            )
            .build()
    }

    /**
     * Builds the top navigation tabs in Android Auto.
     * Android Auto displays root children directly as tabs in the head unit navigation.
     * Order specified by user: Recent & Favorites are the first 2 tabs!
     */
    fun buildTopLevelTabs(): List<MediaItem> {
        return listOf(
            createTabItem(TAB_RECENT, "Recent", "Recently played stations"),
            createTabItem(TAB_FAVORITES, "Favorites", "Your starred stations"),
            createTabItem(TAB_LOCAL, "Local", "Trending streams in your country"),
            createTabItem(TAB_WORLD, "World", "Browse countries & top voted"),
            createTabItem(TAB_POPULAR, "Popular", "Top voted stations worldwide"),
            createTabItem(TAB_HIDDEN, "Hidden", "Hidden stations")
        )
    }

    private fun createTabItem(id: String, title: String, subtitle: String): MediaItem {
        val tabExtras = Bundle().apply {
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                    .setExtras(tabExtras)
                    .build()
            )
            .build()
    }

    fun countryToMediaItem(country: Country): MediaItem {
        val countryExtras = Bundle().apply {
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
        }
        return MediaItem.Builder()
            .setMediaId("$PREFIX_COUNTRY${country.code}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(country.name)
                    .setSubtitle("${country.stationCount} stations")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setFolderType(MediaMetadata.FOLDER_TYPE_GENRES)
                    .setExtras(countryExtras)
                    .build()
            )
            .build()
    }

    fun stationToMediaItem(station: Station, parentId: String? = null): MediaItem {
        val extras = Bundle().apply {
            putString("station_id", station.id)
            putString("stream_url", station.streamUrl)
            putString("country", station.country ?: "")
            putString("country_code", station.countryCode ?: "")
            putString("codec", station.codec ?: "")
            putInt("bitrate", station.bitrate)
            putBoolean("is_favorite", station.isFavorite)
            putBoolean("is_hidden", station.isHidden)
            if (!parentId.isNullOrBlank()) {
                putString("parent_id", parentId)
            }
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.country ?: "Online Radio")
            .setAlbumTitle(station.tags.take(3).joinToString(" • "))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(extras)

        if (!station.favicon.isNullOrBlank()) {
            metadataBuilder.setArtworkUri(Uri.parse(station.favicon))
        }

        val compoundMediaId = buildMediaId(parentId, station.id)

        return MediaItem.Builder()
            .setMediaId(compoundMediaId)
            .setUri(Uri.parse(station.streamUrl))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun mediaItemToStation(mediaItem: MediaItem): Station {
        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras

        val rawId = extras?.getString("station_id") ?: mediaItem.mediaId
        val (_, pureStationId) = parseMediaId(rawId)
        val streamUrl = extras?.getString("stream_url") ?: mediaItem.requestMetadata.mediaUri?.toString() ?: ""
        val country = extras?.getString("country") ?: metadata.artist?.toString()
        val countryCode = extras?.getString("country_code")
        val codec = extras?.getString("codec")
        val bitrate = extras?.getInt("bitrate", 0) ?: 0
        val isFavorite = extras?.getBoolean("is_favorite", false) ?: false
        val isHidden = extras?.getBoolean("is_hidden", false) ?: false

        return Station(
            id = pureStationId,
            name = metadata.title?.toString() ?: "Radio Station",
            streamUrl = streamUrl,
            favicon = metadata.artworkUri?.toString(),
            country = country,
            countryCode = countryCode,
            codec = codec,
            bitrate = bitrate,
            isFavorite = isFavorite,
            isHidden = isHidden
        )
    }
}
