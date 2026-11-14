package com.worldradio.auto.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldradio.auto.R
import com.worldradio.auto.ui.components.DiagnosticsDialog
import com.worldradio.auto.ui.components.MiniPlayer
import com.worldradio.auto.ui.components.StationListItem
import com.worldradio.auto.ui.theme.DarkCardBorder
import com.worldradio.auto.ui.theme.DarkCardElevated
import com.worldradio.auto.ui.theme.DarkCardSurface
import com.worldradio.auto.ui.theme.DeepObsidian
import com.worldradio.auto.ui.theme.ElectricEmerald
import com.worldradio.auto.ui.theme.TextHighEmphasis
import com.worldradio.auto.ui.theme.TextLowEmphasis
import com.worldradio.auto.ui.theme.TextMediumEmphasis
import com.worldradio.auto.ui.viewmodel.RadioViewModel
import kotlinx.coroutines.launch

data class TabInfo(
    val title: String,
    val icon: ImageVector,
    val isAlphabetical: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: RadioViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // Tab items: Recent (#1) and Favorites (#2) are the first two tabs!
    // 6 distinct tabs: Recent (chronological), Favorites (A-Z), Local (stream clicks), World (countries), Popular (worldwide top voted), Hidden (A-Z)
    val tabs = remember {
        listOf(
            TabInfo("Recent", Icons.Default.History, isAlphabetical = false),
            TabInfo("Favorites", Icons.Default.Star, isAlphabetical = true),
            TabInfo("Local", Icons.Default.LocationOn, isAlphabetical = false),
            TabInfo("World", Icons.Default.Public, isAlphabetical = false),
            TabInfo("Popular", Icons.AutoMirrored.Filled.TrendingUp, isAlphabetical = false),
            TabInfo("Hidden", Icons.Default.VisibilityOff, isAlphabetical = true)
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    val currentStation by viewModel.currentStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val canSkipNext by viewModel.canSkipNext.collectAsState()
    val canSkipPrevious by viewModel.canSkipPrevious.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val recentStations by viewModel.recentStations.collectAsState()
    val favoriteStations by viewModel.favoriteStations.collectAsState()
    val localStations by viewModel.localStations.collectAsState()
    val localCountryCode by viewModel.localCountryCode.collectAsState()
    val countries by viewModel.countries.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val countryStations by viewModel.countryStations.collectAsState()
    val popularStations by viewModel.popularStations.collectAsState()
    val hiddenStations by viewModel.hiddenStations.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text(stringResource(R.string.search_hint), color = TextLowEmphasis) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextHighEmphasis,
                                unfocusedTextColor = TextHighEmphasis
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search", tint = TextHighEmphasis)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCardSurface)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "World Radio",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextHighEmphasis,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricEmerald.copy(alpha = 0.15f))
                                    .border(1.dp, ElectricEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUTO",
                                    color = ElectricEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDiagnosticsDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Diagnostics", tint = TextMediumEmphasis)
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextHighEmphasis)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCardSurface)
                )
            }
        },
        bottomBar = {
            MiniPlayer(
                station = currentStation,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                isShuffleEnabled = isShuffleEnabled,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSkipPrevious = { viewModel.skipToPrevious() },
                onSkipNext = { viewModel.skipToNext() },
                onShuffleToggle = { viewModel.toggleShuffleMode() },
                onFavoriteToggle = { currentStation?.let { viewModel.toggleFavorite(it) } },
                onHideToggle = { currentStation?.let { viewModel.toggleHidden(it) } }
            )
        },
        containerColor = DeepObsidian
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isSearchActive && searchQuery.isNotBlank()) {
                val searchListState = rememberLazyListState()

                LaunchedEffect(currentStation?.id) {
                    val stationId = currentStation?.id ?: return@LaunchedEffect
                    val stationIndex = searchResults.indexOfFirst { it.id == stationId }
                    if (stationIndex != -1) {
                        val visibleCount = searchListState.layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 6
                        val itemsBeforeCenter = (visibleCount / 2).coerceAtLeast(1)
                        val scrollTarget = (stationIndex - itemsBeforeCenter).coerceAtLeast(0)

                        val firstVisible = searchListState.firstVisibleItemIndex
                        if (kotlin.math.abs(scrollTarget - firstVisible) > 15) {
                            val preSnap = if (scrollTarget > firstVisible) (scrollTarget - 2).coerceAtLeast(0) else (scrollTarget + 2).coerceAtMost((searchResults.size - 1).coerceAtLeast(0))
                            searchListState.scrollToItem(preSnap)
                        }
                        searchListState.animateScrollToItem(scrollTarget)
                    }
                }

                // Search Results
                LazyColumn(
                    state = searchListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults, key = { it.id }) { station ->
                        StationListItem(
                            station = station,
                            isPlayingThisStation = isPlaying && currentStation?.id == station.id,
                            onStationClick = { viewModel.playStation(station, searchResults) },
                            onFavoriteToggle = { viewModel.toggleFavorite(station) },
                            onHideToggle = { viewModel.toggleHidden(station) }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern Segmented Pill Tabs Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCardSurface
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(tabs) { index, tab ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(if (isSelected) ElectricEmerald else DarkCardElevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) ElectricEmerald else DarkCardBorder,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) DeepObsidian else TextMediumEmphasis,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            color = if (isSelected) DeepObsidian else TextHighEmphasis,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pager Contents
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> RecentScreen(
                                stations = recentStations,
                                currentStation = currentStation,
                                isPlaying = isPlaying,
                                onStationClick = { viewModel.playStation(it, recentStations) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onHideToggle = { viewModel.toggleHidden(it) }
                            )
                            1 -> FavoritesScreen(
                                stations = favoriteStations,
                                currentStation = currentStation,
                                isPlaying = isPlaying,
                                onStationClick = { viewModel.playStation(it, favoriteStations) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onHideToggle = { viewModel.toggleHidden(it) }
                            )
                            2 -> LocalScreen(
                                stations = localStations,
                                currentStation = currentStation,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                currentCountryCode = localCountryCode,
                                onCountryChange = { viewModel.loadLocalStations(it) },
                                onStationClick = { viewModel.playStation(it, localStations) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onHideToggle = { viewModel.toggleHidden(it) }
                            )
                            3 -> WorldScreen(
                                countries = countries,
                                selectedCountry = selectedCountry,
                                stations = countryStations,
                                currentStation = currentStation,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                onCountrySelect = { viewModel.selectCountry(it) },
                                onBackToCountries = { viewModel.clearSelectedCountry() },
                                onStationClick = { viewModel.playStation(it, countryStations) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onHideToggle = { viewModel.toggleHidden(it) }
                            )
                            4 -> PopularScreen(
                                stations = popularStations,
                                currentStation = currentStation,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                onStationClick = { viewModel.playStation(it, popularStations) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onHideToggle = { viewModel.toggleHidden(it) }
                            )
                            5 -> HiddenScreen(
                                stations = hiddenStations,
                                currentStation = currentStation,
                                isPlaying = isPlaying,
                                onStationClick = { viewModel.playStation(it, hiddenStations, allowHidden = true) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onHideToggle = { viewModel.toggleHidden(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDiagnosticsDialog) {
        DiagnosticsDialog(onDismissRequest = { showDiagnosticsDialog = false })
    }
}
