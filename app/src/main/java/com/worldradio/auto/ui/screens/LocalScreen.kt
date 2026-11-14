package com.worldradio.auto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.ui.components.StationListItem
import com.worldradio.auto.ui.theme.DarkCardBorder
import com.worldradio.auto.ui.theme.DarkCardElevated
import com.worldradio.auto.ui.theme.DarkCardSurface
import com.worldradio.auto.ui.theme.DeepObsidian
import com.worldradio.auto.ui.theme.ElectricEmerald
import com.worldradio.auto.ui.theme.TextHighEmphasis
import com.worldradio.auto.ui.theme.TextLowEmphasis
import com.worldradio.auto.ui.theme.TextMediumEmphasis

data class QuickCountry(val code: String, val name: String, val flag: String)

private val QUICK_COUNTRIES = listOf(
    QuickCountry("RO", "Romania", "🇷🇴"),
    QuickCountry("MD", "Moldova", "🇲🇩"),
    QuickCountry("GB", "UK", "🇬🇧"),
    QuickCountry("DE", "Germany", "🇩🇪"),
    QuickCountry("FR", "France", "🇫🇷"),
    QuickCountry("IT", "Italy", "🇮🇹"),
    QuickCountry("ES", "Spain", "🇪🇸"),
    QuickCountry("US", "USA", "🇺🇸")
)

@Composable
fun LocalScreen(
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentCountryCode: String,
    onCountryChange: (String) -> Unit,
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
            val visibleCount = listState.layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 6
            val itemsBeforeCenter = (visibleCount / 2).coerceAtLeast(1)
            val scrollTarget = (stationIndex - itemsBeforeCenter).coerceAtLeast(0)

            val firstVisible = listState.firstVisibleItemIndex
            if (kotlin.math.abs(scrollTarget - firstVisible) > 15) {
                val preSnap = if (scrollTarget > firstVisible) (scrollTarget - 2).coerceAtLeast(0) else (scrollTarget + 2).coerceAtMost((stations.size - 1).coerceAtLeast(0))
                listState.scrollToItem(preSnap)
            }
            listState.animateScrollToItem(scrollTarget)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Modern Header with Active Country & Sorting Mode Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCardSurface
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val countryName = QUICK_COUNTRIES.find { it.code.equals(currentCountryCode, ignoreCase = true) }?.name ?: currentCountryCode
                        Text(
                            text = "Country: $countryName",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextHighEmphasis,
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${stations.size} stations)",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextLowEmphasis, fontSize = 11.5.sp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Stream Clicks Badge (Scenario 3 - Option B)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCardElevated)
                            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = ElectricEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Stream Clicks",
                                color = ElectricEmerald,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick country selector capsule chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(QUICK_COUNTRIES, key = { it.code }) { item ->
                        val isSelected = item.code.equals(currentCountryCode, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) ElectricEmerald else DarkCardElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricEmerald else DarkCardBorder,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onCountryChange(item.code) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.flag} ${item.name}",
                                color = if (isSelected) DeepObsidian else TextHighEmphasis,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Stations List - Scenario 3 Option B (Ordered by Stream Clicks)
        if (isLoading && stations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricEmerald)
            }
        } else if (stations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No stations found for this country.",
                    color = TextMediumEmphasis,
                    fontSize = 13.5.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
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
}
