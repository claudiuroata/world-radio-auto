package com.worldradio.auto.media

import androidx.media3.common.MediaItem
import com.worldradio.auto.data.model.Station

object PlaylistQueueHelper {

    fun filterStationsForQueue(
        stations: List<Station>,
        hiddenIds: Set<String>,
        allowHidden: Boolean = false
    ): List<Station> {
        if (allowHidden || hiddenIds.isEmpty()) return stations
        return stations.filter { it.id !in hiddenIds }
    }

    fun filterMediaItemsForQueue(
        items: List<MediaItem>,
        hiddenIds: Set<String>,
        allowHidden: Boolean = false
    ): List<MediaItem> {
        if (allowHidden || hiddenIds.isEmpty()) return items
        return items.filter { item ->
            val (_, pureId) = MediaItemConverter.parseMediaId(item.mediaId)
            pureId !in hiddenIds
        }
    }

    /**
     * Returns descending indices of queue items matching [stationId] so callers
     * can remove them safely without index shifting.
     */
    fun descendingIndicesMatchingStationId(
        mediaIds: List<String>,
        stationId: String
    ): List<Int> {
        return mediaIds.indices
            .filter { index ->
                MediaItemConverter.parseMediaId(mediaIds[index]).second == stationId
            }
            .sortedDescending()
    }

    /**
     * Builds the "For You" suggested station blend (Favorites -> Recents -> Local/Fallback),
     * deduplicated by station ID, excluding all [hiddenIds], and capped at [limit].
     */
    fun buildSuggestedStations(
        favorites: List<Station>,
        recents: List<Station>,
        localOrFallback: List<Station>,
        hiddenIds: Set<String>,
        limit: Int = 25
    ): List<Station> {
        val combined = ArrayList<Station>(limit)
        val seenIds = HashSet<String>()
        for (source in listOf(favorites, recents, localOrFallback)) {
            for (station in source) {
                if (station.id !in hiddenIds && seenIds.add(station.id)) {
                    combined.add(station)
                    if (combined.size >= limit) return combined
                }
            }
        }
        return combined
    }
}
