package com.worldradio.auto.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldradio.auto.data.model.Country
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

@Composable
fun WorldScreen(
    countries: List<Country>,
    selectedCountry: Country?,
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onCountrySelect: (Country) -> Unit,
    onBackToCountries: () -> Unit,
    onStationClick: (Station) -> Unit,
    onFavoriteToggle: (Station) -> Unit,
    onHideToggle: (Station) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Intercept physical or gesture back button when inside a country's stations page
    if (selectedCountry != null) {
        BackHandler {
            onBackToCountries()
        }
    }

    if (selectedCountry == null) {
        // ==========================================
        // PAGE 1: COUNTRIES DIRECTORY (FILTER & BROWSE)
        // ==========================================
        CountriesDirectoryPage(
            countries = countries,
            onCountrySelect = onCountrySelect,
            modifier = modifier
        )
    } else {
        // ==========================================
        // PAGE 2: DEDICATED COUNTRY STATIONS PAGE
        // ==========================================
        CountryStationsPage(
            country = selectedCountry,
            stations = stations,
            currentStation = currentStation,
            isPlaying = isPlaying,
            isLoading = isLoading,
            onBackClick = onBackToCountries,
            onStationClick = onStationClick,
            onFavoriteToggle = onFavoriteToggle,
            onHideToggle = onHideToggle,
            modifier = modifier
        )
    }
}

@Composable
private fun CountriesDirectoryPage(
    countries: List<Country>,
    onCountrySelect: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var minStationFilter by remember { mutableStateOf(0) } // 0 = all, 50, 100

    val filteredCountries = remember(countries, searchQuery, minStationFilter) {
        countries
            .filter { country ->
                val matchesQuery = searchQuery.isBlank() ||
                        country.name.contains(searchQuery, ignoreCase = true) ||
                        country.code.contains(searchQuery, ignoreCase = true)
                val matchesMinCount = country.stationCount >= minStationFilter
                matchesQuery && matchesMinCount
            }
            .sortedBy { it.name.lowercase() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with Search and Hide/Show Filter Chips
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Browse by Country",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextHighEmphasis,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filteredCountries.size})",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextLowEmphasis, fontSize = 12.sp)
                        )
                    }

                    // A-Z Sort Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCardElevated)
                            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = null,
                                tint = ElectricEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "A-Z",
                                color = ElectricEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search/Filter countries input box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter countries (e.g. Romania, UK, France...)", color = TextLowEmphasis, fontSize = 12.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMediumEmphasis, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMediumEmphasis, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextHighEmphasis,
                            unfocusedTextColor = TextHighEmphasis
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Hide/Show Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterPill(
                            label = "All Countries",
                            isSelected = minStationFilter == 0,
                            onClick = { minStationFilter = 0 }
                        )
                    }
                    item {
                        FilterPill(
                            label = "50+ Stations",
                            isSelected = minStationFilter == 50,
                            onClick = { minStationFilter = if (minStationFilter == 50) 0 else 50 }
                        )
                    }
                    item {
                        FilterPill(
                            label = "100+ Stations",
                            isSelected = minStationFilter == 100,
                            onClick = { minStationFilter = if (minStationFilter == 100) 0 else 100 }
                        )
                    }
                }
            }
        }

        // Alphabetical Countries List Cards
        if (filteredCountries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No countries match \"$searchQuery\"", color = TextMediumEmphasis)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredCountries, key = { it.code }) { country ->
                    CountryCardItem(
                        country = country,
                        onClick = { onCountrySelect(country) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ElectricEmerald else DarkCardElevated)
            .border(
                width = 1.dp,
                color = if (isSelected) ElectricEmerald else DarkCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) DeepObsidian else TextMediumEmphasis,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CountryCardItem(
    country: Country,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = DarkCardSurface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Flag representation or country code icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCardElevated)
                        .border(0.5.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countryCodeToEmoji(country.code),
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = country.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextHighEmphasis,
                            fontSize = 14.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${country.code.uppercase()} • ${country.stationCount} online radios",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMediumEmphasis,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${country.name}",
                tint = TextMediumEmphasis,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CountryStationsPage(
    country: Country,
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onFavoriteToggle: (Station) -> Unit,
    onHideToggle: (Station) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var stationSearch by remember { mutableStateOf("") }

    val filteredStations = remember(stations, stationSearch) {
        if (stationSearch.isBlank()) stations
        else stations.filter {
            it.name.contains(stationSearch, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(stationSearch, ignoreCase = true) }
        }
    }

    LaunchedEffect(currentStation?.id) {
        val stationId = currentStation?.id ?: return@LaunchedEffect
        val stationIndex = filteredStations.indexOfFirst { it.id == stationId }
        if (stationIndex != -1) {
            val visibleCount = listState.layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 6
            val itemsBeforeCenter = (visibleCount / 2).coerceAtLeast(1)
            val scrollTarget = (stationIndex - itemsBeforeCenter).coerceAtLeast(0)

            val firstVisible = listState.firstVisibleItemIndex
            if (kotlin.math.abs(scrollTarget - firstVisible) > 15) {
                val preSnap = if (scrollTarget > firstVisible) (scrollTarget - 2).coerceAtLeast(0) else (scrollTarget + 2).coerceAtMost((filteredStations.size - 1).coerceAtLeast(0))
                listState.scrollToItem(preSnap)
            }
            listState.animateScrollToItem(scrollTarget)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky Header with Back Button and Country Title
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
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCardElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to countries",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = countryCodeToEmoji(country.code),
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = country.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextHighEmphasis,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stations.size} stations available",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextLowEmphasis,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Top Voted badge (Scenario 2)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCardElevated)
                            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "★ Top Voted",
                                color = ElectricEmerald,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Station Search Filter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                ) {
                    TextField(
                        value = stationSearch,
                        onValueChange = { stationSearch = it },
                        placeholder = { Text("Filter stations in ${country.name}...", color = TextLowEmphasis, fontSize = 12.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMediumEmphasis, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (stationSearch.isNotEmpty()) {
                                IconButton(onClick = { stationSearch = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMediumEmphasis, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextHighEmphasis,
                            unfocusedTextColor = TextHighEmphasis
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    )
                }
            }
        }

        // Stations List
        if (isLoading && stations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricEmerald)
            }
        } else if (filteredStations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No stations match your filter in ${country.name}.", color = TextMediumEmphasis)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredStations, key = { it.id }) { station ->
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

/**
 * Converts a 2-letter ISO country code (e.g. "RO", "US", "GB") to its Flag Emoji (🇷🇴, 🇺🇸, 🇬🇧)
 */
fun countryCodeToEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"
    val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}
