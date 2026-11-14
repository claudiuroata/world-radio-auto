package com.worldradio.auto.media

import androidx.media3.common.MediaMetadata
import com.worldradio.auto.data.model.Country
import com.worldradio.auto.data.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaItemConverterTest {

    @Test
    fun buildMediaIdAndParseMediaId_handleWithParentNullAndBlank() {
        val withParent = MediaItemConverter.buildMediaId(MediaItemConverter.TAB_LOCAL, "st_42")
        assertEquals("[TAB_LOCAL]||st_42", withParent)
        assertEquals(Pair("[TAB_LOCAL]", "st_42"), MediaItemConverter.parseMediaId(withParent))

        val withNullParent = MediaItemConverter.buildMediaId(null, "st_42")
        assertEquals("st_42", withNullParent)
        assertEquals(Pair(null, "st_42"), MediaItemConverter.parseMediaId(withNullParent))

        val withBlankParent = MediaItemConverter.buildMediaId("   ", "st_42")
        assertEquals("st_42", withBlankParent)
    }

    @Test
    fun rootAndSuggestedItems_haveCorrectMediaIdsAndBrowsableFlags() {
        val root = MediaItemConverter.buildRootItem()
        assertEquals(MediaItemConverter.ROOT_ID, root.mediaId)
        assertEquals("World Radio Auto", root.mediaMetadata.title.toString())
        assertTrue(root.mediaMetadata.isBrowsable == true)
        assertFalse(root.mediaMetadata.isPlayable == true)
        assertEquals(
            MediaItemConverter.CONTENT_STYLE_LIST,
            root.mediaMetadata.extras?.getInt(MediaItemConverter.CONTENT_STYLE_BROWSABLE_HINT)
        )

        val suggested = MediaItemConverter.buildSuggestedRootItem()
        assertEquals(MediaItemConverter.TAB_SUGGESTED, suggested.mediaId)
        assertEquals("For You", suggested.mediaMetadata.title.toString())
        assertTrue(suggested.mediaMetadata.isBrowsable == true)
        assertFalse(suggested.mediaMetadata.isPlayable == true)
        assertEquals(true, suggested.mediaMetadata.extras?.getBoolean("android.service.media.extra.SUGGESTED"))
    }

    @Test
    fun buildTopLevelTabs_includesPopularAndHiddenTabsWithGridPlayableStyle() {
        val tabs = MediaItemConverter.buildTopLevelTabs()
        assertEquals(
            listOf(
                MediaItemConverter.TAB_RECENT,
                MediaItemConverter.TAB_FAVORITES,
                MediaItemConverter.TAB_LOCAL,
                MediaItemConverter.TAB_WORLD,
                MediaItemConverter.TAB_POPULAR,
                MediaItemConverter.TAB_HIDDEN
            ),
            tabs.map { it.mediaId }
        )

        val popularTab = tabs[4]
        assertEquals("Popular", popularTab.mediaMetadata.title.toString())
        assertTrue(popularTab.mediaMetadata.isBrowsable == true)
        assertFalse(popularTab.mediaMetadata.isPlayable == true)

        val hiddenTab = tabs.last()
        assertEquals("Hidden", hiddenTab.mediaMetadata.title.toString())
        assertTrue(hiddenTab.mediaMetadata.isBrowsable == true)
        assertFalse(hiddenTab.mediaMetadata.isPlayable == true)
        assertEquals(
            MediaItemConverter.CONTENT_STYLE_GRID,
            hiddenTab.mediaMetadata.extras?.getInt(MediaItemConverter.CONTENT_STYLE_PLAYABLE_HINT)
        )
    }

    @Test
    fun countryToMediaItem_setsCountryPrefixAndSubtitle() {
        val single = MediaItemConverter.countryToMediaItem(Country("Monaco", "MC", 1))
        assertEquals("COUNTRY_MC", single.mediaId)
        assertEquals("1 stations", single.mediaMetadata.subtitle.toString())
        assertTrue(single.mediaMetadata.isBrowsable == true)
        assertFalse(single.mediaMetadata.isPlayable == true)

        val plural = MediaItemConverter.countryToMediaItem(Country("Romania", "RO", 150))
        assertEquals("COUNTRY_RO", plural.mediaId)
        assertEquals("150 stations", plural.mediaMetadata.subtitle.toString())
    }

    @Test
    fun stationToMediaItemAndMediaItemToStation_roundTripsAllMetadataAndExtras() {
        val station = Station(
            id = "ro_1",
            name = "Europa FM",
            streamUrl = "https://astreaming.europafm.ro:8443/europafm_aacp48k",
            favicon = "https://europafm.ro/icon.png",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("Pop", "News", "Hits", "Extra"),
            codec = "AAC",
            bitrate = 64,
            isFavorite = true,
            isHidden = true
        )

        val item = MediaItemConverter.stationToMediaItem(
            station = station,
            parentId = MediaItemConverter.TAB_HIDDEN
        )

        assertEquals("[TAB_HIDDEN]||ro_1", item.mediaId)
        assertEquals(station.name, item.mediaMetadata.title.toString())
        assertEquals("Romania", item.mediaMetadata.artist.toString())
        assertEquals("Pop • News • Hits", item.mediaMetadata.albumTitle.toString())
        assertEquals(station.streamUrl, item.localConfiguration?.uri.toString())
        assertTrue(item.mediaMetadata.isPlayable == true)
        assertFalse(item.mediaMetadata.isBrowsable == true)
        assertEquals(MediaMetadata.MEDIA_TYPE_RADIO_STATION, item.mediaMetadata.mediaType)
        assertEquals(MediaItemConverter.TAB_HIDDEN, item.mediaMetadata.extras?.getString("parent_id"))
        assertEquals(true, item.mediaMetadata.extras?.getBoolean("is_hidden"))
        assertEquals(true, item.mediaMetadata.extras?.getBoolean("is_favorite"))

        val parsed = MediaItemConverter.mediaItemToStation(item)
        assertEquals("ro_1", parsed.id)
        assertEquals(station.name, parsed.name)
        assertEquals(station.streamUrl, parsed.streamUrl)
        assertEquals(station.favicon, parsed.favicon)
        assertEquals(station.country, parsed.country)
        assertEquals(station.countryCode, parsed.countryCode)
        assertEquals(station.codec, parsed.codec)
        assertEquals(station.bitrate, parsed.bitrate)
        assertTrue(parsed.isFavorite)
        assertTrue(parsed.isHidden)
    }
}
