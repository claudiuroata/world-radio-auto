package com.worldradio.auto.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.ui.components.StationListItem
import com.worldradio.auto.ui.theme.ElectricEmerald
import com.worldradio.auto.ui.theme.TextMediumEmphasis

@Composable
fun PopularScreen(
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onStationClick: (Station) -> Unit,
    onFavoriteToggle: (Station) -> Unit,
    onHideToggle: (Station) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isLoading && stations.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricEmerald)
        }
    } else if (stations.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No popular stations found.", color = TextMediumEmphasis)
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Voted Worldwide (Popularity Ranked)",
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
