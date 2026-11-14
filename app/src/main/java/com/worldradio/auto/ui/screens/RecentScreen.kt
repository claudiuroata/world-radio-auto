package com.worldradio.auto.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldradio.auto.R
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.ui.components.StationListItem
import com.worldradio.auto.ui.theme.TextLowEmphasis
import com.worldradio.auto.ui.theme.TextMediumEmphasis

@Composable
fun RecentScreen(
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    onStationClick: (Station) -> Unit,
    onFavoriteToggle: (Station) -> Unit,
    onHideToggle: (Station) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentStation?.id) {
        val stationId = currentStation?.id ?: return@LaunchedEffect
        val stationIndex = stations.indexOfFirst { it.id == stationId }
        if (stationIndex != -1) {
            val targetIndex = stationIndex + 1 // +1 for header item
            val visibleCount = listState.layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 6
            val itemsBeforeCenter = (visibleCount / 2).coerceAtLeast(1)
            val scrollTarget = (targetIndex - itemsBeforeCenter).coerceAtLeast(0)

            val firstVisible = listState.firstVisibleItemIndex
            if (kotlin.math.abs(scrollTarget - firstVisible) > 15) {
                val preSnap = if (scrollTarget > firstVisible) (scrollTarget - 2).coerceAtLeast(0) else (scrollTarget + 2).coerceAtMost(stations.size)
                listState.scrollToItem(preSnap)
            }
            listState.animateScrollToItem(scrollTarget)
        }
    }

    if (stations.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextLowEmphasis,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_recents),
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextMediumEmphasis),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Listened (Chronological)",
                        color = TextMediumEmphasis,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            items(stations, key = { it.id }) { station ->
                StationListItem(
                    station = station,
                    isPlayingThisStation = isPlaying && currentStation?.id == station.id,
                    onStationClick = { onStationClick(station) },
                    onFavoriteToggle = { onFavoriteToggle(station) },
                    onHideToggle = { onHideToggle(station) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}
