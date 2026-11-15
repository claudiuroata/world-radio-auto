package com.worldradio.auto.media

import com.worldradio.auto.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioMediaServiceCustomLayoutTest {

    @Test
    fun buildCustomLayoutButtons_placesFavoriteThenHideThenShuffle() {
        val buttons = RadioMediaService.buildCustomLayoutButtons(
            isFavorite = false,
            isHidden = false,
            isShuffled = false
        )

        assertEquals(
            listOf(
                RadioMediaService.ACTION_TOGGLE_FAVORITE,
                RadioMediaService.ACTION_TOGGLE_HIDE,
                RadioMediaService.ACTION_TOGGLE_SHUFFLE
            ),
            buttons.map { it.sessionCommand?.customAction }
        )
        assertEquals(listOf("Favorite", "Hide", "Shuffle: OFF"), buttons.map { it.displayName.toString() })
        assertEquals(
            listOf(R.drawable.ic_star_off, R.drawable.ic_hide_off, R.drawable.ic_shuffle_off),
            buttons.map { it.iconResId }
        )
    }

    @Test
    fun buildCustomLayoutButtons_reflectsActiveToggleStates() {
        val buttons = RadioMediaService.buildCustomLayoutButtons(
            isFavorite = true,
            isHidden = true,
            isShuffled = true
        )

        assertEquals(listOf("Favorited", "Unhide", "Shuffle: ON"), buttons.map { it.displayName.toString() })
        assertEquals(
            listOf(R.drawable.ic_star_on, R.drawable.ic_hide_on, R.drawable.ic_shuffle_on),
            buttons.map { it.iconResId }
        )
    }
}
