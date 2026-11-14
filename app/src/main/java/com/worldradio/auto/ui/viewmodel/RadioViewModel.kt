package com.worldradio.auto.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.worldradio.auto.RadioApp
import com.worldradio.auto.data.model.Country
import com.worldradio.auto.data.model.CuratedStations
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.data.repository.RadioRepository
import com.worldradio.auto.media.MediaItemConverter
import com.worldradio.auto.media.PlaylistQueueHelper
import com.worldradio.auto.media.RadioMediaService
import com.worldradio.auto.util.CountryDetector
import com.worldradio.auto.util.UserLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class RadioViewModel internal constructor(
    application: Application,
    private val repository: RadioRepository,
    autoConnect: Boolean
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = (application as RadioApp).repository,
        autoConnect = true
    )

    // MediaController connection
    private var mediaController: MediaController? = null

    val favoriteStations: StateFlow<List<Station>> = repository.getFavoriteStations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenStations: StateFlow<List<Station>> = repository.getHiddenStations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Live set of ALL favorite IDs from Room (including hidden stations so MiniPlayer star reflects true DB state)
    private val favoriteIds: StateFlow<Set<String>> = repository.getAllFavoriteStationIds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // Live set of hidden IDs from Room - single source of truth for excluding hidden stations
    private val hiddenIds: StateFlow<Set<String>> = hiddenStations
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Re-applies favorite state and excludes hidden stations from snapshot lists */
    private fun withUserFlags(source: StateFlow<List<Station>>): StateFlow<List<Station>> =
        combine(source, favoriteIds, hiddenIds) { list, favIds, hidIds ->
            list.filter { it.id !in hidIds }
                .map { it.copy(isFavorite = it.id in favIds, isHidden = false) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentStations: StateFlow<List<Station>> = repository.getRecentStations(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localStations = MutableStateFlow<List<Station>>(emptyList())
    val localStations: StateFlow<List<Station>> = withUserFlags(_localStations)

    private val _localCountryCode = MutableStateFlow("RO")
    val localCountryCode: StateFlow<String> = _localCountryCode.asStateFlow()

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries.asStateFlow()

    private val _selectedCountry = MutableStateFlow<Country?>(null)
    val selectedCountry: StateFlow<Country?> = _selectedCountry.asStateFlow()

    private val _countryStations = MutableStateFlow<List<Station>>(emptyList())
    val countryStations: StateFlow<List<Station>> = withUserFlags(_countryStations)

    private val _currentStation = MutableStateFlow<Station?>(null)
    val currentStation: StateFlow<Station?> = combine(_currentStation, favoriteIds, hiddenIds) { station, favIds, hidIds ->
        station?.copy(
            isFavorite = station.id in favIds,
            isHidden = station.id in hidIds
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _canSkipNext = MutableStateFlow(false)
    val canSkipNext: StateFlow<Boolean> = _canSkipNext.asStateFlow()

    private val _canSkipPrevious = MutableStateFlow(false)
    val canSkipPrevious: StateFlow<Boolean> = _canSkipPrevious.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Station>>(emptyList())
    val searchResults: StateFlow<List<Station>> = withUserFlags(_searchResults)

    private var searchJob: Job? = null

    init {
        if (autoConnect) {
            connectMediaController()
            val detected = CountryDetector.detectCountryCode(application)
            _localCountryCode.value = detected
            loadLocalStations(detected)
            loadCountries()

            // Background IP check to verify country when roaming / Wi-Fi
            viewModelScope.launch {
                CountryDetector.fetchIpLocation(application)?.let { ipLoc ->
                    if (ipLoc.countryCode.isNotBlank() && ipLoc.countryCode != _localCountryCode.value) {
                        _localCountryCode.value = ipLoc.countryCode
                        loadLocalStations(ipLoc.countryCode)
                    }
                }
            }
        }
    }

    private fun connectMediaController() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, RadioMediaService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                setupPlayerListener(controller)
                syncCurrentPlaybackState(controller)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, context.mainExecutor)
    }

    private fun updateSkipAvailability(player: Player) {
        val count = player.mediaItemCount
        _canSkipNext.value = count > 1 || player.hasNextMediaItem()
        _canSkipPrevious.value = count > 1 || player.hasPreviousMediaItem()
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    _currentStation.value = MediaItemConverter.mediaItemToStation(mediaItem)
                }
                updateSkipAvailability(player)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleModeEnabled
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateSkipAvailability(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                _isPlaying.value = false
                _isBuffering.value = false
                updateSkipAvailability(player)
            }
        })
    }

    private fun syncCurrentPlaybackState(player: Player) {
        _isPlaying.value = player.isPlaying
        _isBuffering.value = (player.playbackState == Player.STATE_BUFFERING)
        _isShuffleEnabled.value = player.shuffleModeEnabled
        player.currentMediaItem?.let {
            _currentStation.value = MediaItemConverter.mediaItemToStation(it)
        }
        updateSkipAvailability(player)
    }

    fun playStation(
        station: Station,
        contextStations: List<Station> = emptyList(),
        allowHidden: Boolean = false
    ) {
        if (!allowHidden && station.id in hiddenIds.value) return
        _currentStation.value = station
        val rawPlaylist = if (contextStations.isNotEmpty() && contextStations.any { it.id == station.id }) {
            contextStations
        } else {
            listOf(station)
        }
        val playlist = PlaylistQueueHelper.filterStationsForQueue(
            stations = rawPlaylist,
            hiddenIds = hiddenIds.value,
            allowHidden = allowHidden
        ).ifEmpty { listOf(station) }

        val startIndex = playlist.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        val parentId = if (allowHidden) MediaItemConverter.TAB_HIDDEN else null
        val mediaItems = playlist.map { MediaItemConverter.stationToMediaItem(it, parentId) }

        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
            updateSkipAvailability(controller)
        }

        viewModelScope.launch {
            repository.recordPlayed(station)
        }
    }

    fun skipToNext() {
        mediaController?.let { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (controller.mediaItemCount > 1) {
                controller.seekToDefaultPosition(0)
            }
        }
    }

    fun skipToPrevious() {
        mediaController?.let { controller ->
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else if (controller.mediaItemCount > 1) {
                controller.seekToDefaultPosition(controller.mediaItemCount - 1)
            }
        }
    }

    fun toggleShuffleMode() {
        mediaController?.let { controller ->
            val newMode = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newMode
            _isShuffleEnabled.value = newMode
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            // Room emits the updated favorites -> every star icon refreshes automatically
            repository.toggleFavorite(station)
        }
    }

    fun toggleHidden(station: Station) {
        viewModelScope.launch {
            val isNowHidden = repository.toggleHidden(station)
            if (isNowHidden) {
                mediaController?.let { controller ->
                    val mediaIds = (0 until controller.mediaItemCount).map {
                        controller.getMediaItemAt(it).mediaId
                    }
                    val indices = PlaylistQueueHelper.descendingIndicesMatchingStationId(mediaIds, station.id)
                    for (index in indices) {
                        controller.removeMediaItem(index)
                    }
                    if (controller.mediaItemCount == 0) {
                        controller.stop()
                        _currentStation.value = null
                        _isPlaying.value = false
                    }
                    updateSkipAvailability(controller)
                }
            }
        }
    }

    fun loadLocalStations(countryCode: String? = null) {
        val code = (countryCode ?: _localCountryCode.value).uppercase().trim().ifBlank { "RO" }
        _localCountryCode.value = code
        viewModelScope.launch {
            _isLoading.value = true
            repository.getLocalStations(code)
                .onSuccess { stations ->
                    _localStations.value = if (stations.isNotEmpty()) stations else CuratedStations.ROMANIA_STARTER_STATIONS
                }
                .onFailure {
                    if (_localStations.value.isEmpty()) {
                        _localStations.value = CuratedStations.ROMANIA_STARTER_STATIONS
                    }
                }
            _isLoading.value = false
        }
    }

    internal fun loadCountries() {
        viewModelScope.launch {
            repository.getCountries()
                .onSuccess { list ->
                    _countries.value = if (list.isNotEmpty()) list else CuratedStations.FALLBACK_COUNTRIES
                }
                .onFailure {
                    if (_countries.value.isEmpty()) {
                        _countries.value = CuratedStations.FALLBACK_COUNTRIES
                    }
                }
        }
    }

    fun selectCountry(country: Country) {
        _selectedCountry.value = country
        viewModelScope.launch {
            _isLoading.value = true
            repository.getStationsByCountry(country.code)
                .onSuccess { stations ->
                    _countryStations.value = if (stations.isNotEmpty()) {
                        stations
                    } else if (country.code.equals("RO", ignoreCase = true)) {
                        CuratedStations.ROMANIA_STARTER_STATIONS
                    } else {
                        emptyList()
                    }
                }
                .onFailure {
                    if (country.code.equals("RO", ignoreCase = true)) {
                        _countryStations.value = CuratedStations.ROMANIA_STARTER_STATIONS
                    }
                }
            _isLoading.value = false
        }
    }

    fun clearSelectedCountry() {
        _selectedCountry.value = null
        _countryStations.value = emptyList()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300)
                _isLoading.value = true
                repository.searchStations(query)
                    .onSuccess { _searchResults.value = it }
                _isLoading.value = false
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
