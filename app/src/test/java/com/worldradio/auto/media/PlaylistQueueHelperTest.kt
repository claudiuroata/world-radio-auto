package com.worldradio.auto.media

import com.worldradio.auto.data.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistQueueHelperTest {

    private fun station(id: String, name: String = "Station $id") = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example.com/$id.mp3"
    )

    @Test
    fun filterStationsForQueue_excludesHiddenWhenNotAllowedAndKeepsWhenAllowed() {
        val all = listOf(station("s1"), station("s2"), station("s3"))
        val hiddenIds = setOf("s2")

        assertEquals(
            listOf("s1", "s3"),
            PlaylistQueueHelper.filterStationsForQueue(all, hiddenIds, allowHidden = false).map { it.id }
        )
        assertEquals(
            listOf("s1", "s2", "s3"),
            PlaylistQueueHelper.filterStationsForQueue(all, hiddenIds, allowHidden = true).map { it.id }
        )
        assertEquals(
            listOf("s1", "s2", "s3"),
            PlaylistQueueHelper.filterStationsForQueue(all, emptySet(), allowHidden = false).map { it.id }
        )
    }

    @Test
    fun filterMediaItemsForQueue_parsesCompoundMediaIdsAndFiltersHidden() {
        val items = listOf(
            MediaItemConverter.stationToMediaItem(station("s1"), MediaItemConverter.TAB_LOCAL),
            MediaItemConverter.stationToMediaItem(station("s2"), MediaItemConverter.TAB_LOCAL),
            MediaItemConverter.stationToMediaItem(station("s3"), "COUNTRY_DE")
        )

        val filtered = PlaylistQueueHelper.filterMediaItemsForQueue(
            items = items,
            hiddenIds = setOf("s2"),
            allowHidden = false
        )
        assertEquals(listOf("[TAB_LOCAL]||s1", "COUNTRY_DE||s3"), filtered.map { it.mediaId })

        val allowed = PlaylistQueueHelper.filterMediaItemsForQueue(
            items = items,
            hiddenIds = setOf("s2"),
            allowHidden = true
        )
        assertEquals(3, allowed.size)
    }

    @Test
    fun descendingIndicesMatchingStationId_handlesCompoundIdsDuplicatesAndSafeRemoval() {
        val mediaIds = listOf(
            "[TAB_LOCAL]||s1",
            "[TAB_LOCAL]||s2",
            "COUNTRY_RO||s1",
            "s3",
            "s1"
        )

        val indices = PlaylistQueueHelper.descendingIndicesMatchingStationId(mediaIds, "s1")
        assertEquals(listOf(4, 2, 0), indices)

        // Simulating ExoPlayer/MediaController sequential removeMediaItem(index)
        val mutableQueue = mediaIds.toMutableList()
        indices.forEach { mutableQueue.removeAt(it) }
        assertEquals(listOf("[TAB_LOCAL]||s2", "s3"), mutableQueue)

        assertTrue(PlaylistQueueHelper.descendingIndicesMatchingStationId(mediaIds, "non_existent").isEmpty())
    }

    @Test
    fun buildSuggestedStations_preservesOrderDeduplicatesExcludesHiddenAndCapsLimit() {
        val favs = listOf(station("f1"), station("shared"), station("hid_fav"))
        val recents = listOf(station("shared"), station("r1"), station("hid_rec"), station("r2"))
        val local = listOf(station("r1"), station("l1"), station("hid_loc"), station("l2"), station("l3"))
        val hiddenIds = setOf("hid_fav", "hid_rec", "hid_loc")

        val suggested = PlaylistQueueHelper.buildSuggestedStations(
            favorites = favs,
            recents = recents,
            localOrFallback = local,
            hiddenIds = hiddenIds,
            limit = 25
        )
        assertEquals(
            listOf("f1", "shared", "r1", "r2", "l1", "l2", "l3"),
            suggested.map { it.id }
        )

        // Verify limit cap works properly
        val capped = PlaylistQueueHelper.buildSuggestedStations(
            favorites = favs,
            recents = recents,
            localOrFallback = local,
            hiddenIds = hiddenIds,
            limit = 3
        )
        assertEquals(listOf("f1", "shared", "r1"), capped.map { it.id })
    }
}
