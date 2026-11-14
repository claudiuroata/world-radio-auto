package com.worldradio.auto.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.worldradio.auto.R
import com.worldradio.auto.RadioApp
import com.worldradio.auto.data.model.CuratedStations
import com.worldradio.auto.ui.MainActivity
import com.worldradio.auto.util.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class RadioMediaService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer

    private val playlistCache = java.util.concurrent.ConcurrentHashMap<String, List<MediaItem>>()
    private val cachedFavoriteIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val cachedHiddenIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    @Volatile private var lastSearchResults: List<MediaItem> = emptyList()

    companion object {
        const val ACTION_TOGGLE_FAVORITE = "com.worldradio.auto.ACTION_TOGGLE_FAVORITE"
        val COMMAND_TOGGLE_FAVORITE = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)

        const val ACTION_TOGGLE_SHUFFLE = "com.worldradio.auto.ACTION_TOGGLE_SHUFFLE"
        val COMMAND_TOGGLE_SHUFFLE = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)

        const val ACTION_TOGGLE_HIDE = "com.worldradio.auto.ACTION_TOGGLE_HIDE"
        val COMMAND_TOGGLE_HIDE = SessionCommand(ACTION_TOGGLE_HIDE, Bundle.EMPTY)
    }

    override fun onCreate() {
        super.onCreate()
        DiagnosticLogger.log("RadioMediaService", "onCreate() MediaLibraryService initialized")
        initializePlayer()
        initializeSession()

        // Observe all favorite IDs (including hidden) to keep Android Auto custom layout Star button accurately updated
        serviceScope.launch {
            RadioApp.instance.repository.getAllFavoriteStationIds().collect { ids ->
                cachedFavoriteIds.clear()
                cachedFavoriteIds.addAll(ids)
                updateCustomLayoutGlobally()
            }
        }

        // Observe hidden stations so Android Auto queues & custom layout Hide button stay synchronized
        serviceScope.launch {
            var initialized = false
            RadioApp.instance.repository.getHiddenStations().collect { list ->
                val nextIds = list.map { it.id }.toSet()
                val changed = cachedHiddenIds.toSet() != nextIds
                cachedHiddenIds.clear()
                cachedHiddenIds.addAll(nextIds)
                if (initialized && changed) {
                    invalidateAllBrowseCaches()
                }
                initialized = true
                updateCustomLayoutGlobally()
            }
        }
    }

    private fun invalidateAllBrowseCaches() {
        val keys = playlistCache.keys.toList()
        playlistCache.clear()
        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_RECENT, 30, null)
        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_FAVORITES, 100, null)
        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_LOCAL, 100, null)
        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_SUGGESTED, 25, null)
        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_POPULAR, 70, null)
        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_HIDDEN, 100, null)
        keys.filter { it.startsWith(MediaItemConverter.PREFIX_COUNTRY) }.forEach { countryKey ->
            mediaLibrarySession?.notifyChildrenChanged(countryKey, 100, null)
        }
    }

    private fun removeStationFromPlayerQueue(stationId: String) {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val mediaIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val indicesToRemove = PlaylistQueueHelper.descendingIndicesMatchingStationId(mediaIds, stationId)
        for (index in indicesToRemove) {
            player.removeMediaItem(index)
        }
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("WorldRadioAuto/1.0 (AndroidAuto; ExoPlayer)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(DefaultRenderersFactory(this))
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ALL

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    val station = MediaItemConverter.mediaItemToStation(mediaItem)
                    serviceScope.launch(Dispatchers.IO) {
                        RadioApp.instance.repository.recordPlayed(station)
                        // Invalidate Recents and Suggested cache so next browse loads fresh
                        playlistCache.remove(MediaItemConverter.TAB_RECENT)
                        playlistCache.remove(MediaItemConverter.TAB_SUGGESTED)
                        // Invalidate Recents and For You suggestions in Android Auto
                        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_RECENT, 30, null)
                        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_SUGGESTED, 25, null)
                    }
                    updateCustomLayoutGlobally()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateCustomLayoutGlobally()
            }

            override fun onPlayerError(error: PlaybackException) {
                // Keep player in usable state for internet radio reconnects
                player.prepare()
            }
        })
    }

    private fun updateCustomLayoutGlobally() {
        val layout = buildCustomLayout()
        mediaLibrarySession?.setCustomLayout(layout)
        mediaLibrarySession?.connectedControllers?.forEach { controller ->
            mediaLibrarySession?.setCustomLayout(controller, layout)
        }
    }

    private fun buildCustomLayout(): ImmutableList<CommandButton> {
        val currentMediaItem = if (::player.isInitialized) player.currentMediaItem else null
        val pureStationId = currentMediaItem?.let {
            MediaItemConverter.parseMediaId(it.mediaId).second
        }
        val isFav = pureStationId != null && cachedFavoriteIds.contains(pureStationId)
        val favoriteButton = CommandButton.Builder()
            .setDisplayName(if (isFav) "Favorited" else "Favorite")
            .setIconResId(if (isFav) R.drawable.ic_star_on else R.drawable.ic_star_off)
            .setSessionCommand(COMMAND_TOGGLE_FAVORITE)
            .build()

        val isShuffled = if (::player.isInitialized) player.shuffleModeEnabled else false
        val shuffleButton = CommandButton.Builder()
            .setDisplayName(if (isShuffled) "Shuffle: ON" else "Shuffle: OFF")
            .setIconResId(if (isShuffled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
            .setSessionCommand(COMMAND_TOGGLE_SHUFFLE)
            .build()

        val isHidden = pureStationId != null && cachedHiddenIds.contains(pureStationId)
        val hideButton = CommandButton.Builder()
            .setDisplayName(if (isHidden) "Unhide" else "Hide")
            .setIconResId(if (isHidden) R.drawable.ic_hide_on else R.drawable.ic_hide_off)
            .setSessionCommand(COMMAND_TOGGLE_HIDE)
            .build()

        return ImmutableList.of(favoriteButton, shuffleButton, hideButton)
    }

    private fun initializeSession() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .setCustomLayout(buildCustomLayout())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        DiagnosticLogger.log("RadioMediaService", "onGetSession() package='${controllerInfo.packageName}'")
        return mediaLibrarySession
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            DiagnosticLogger.log(
                "RadioMediaService",
                "onConnect() from package='${controller.packageName}', uid=${controller.uid}"
            )
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(COMMAND_TOGGLE_FAVORITE)
                .add(COMMAND_TOGGLE_SHUFFLE)
                .add(COMMAND_TOGGLE_HIDE)
                .build()

            val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .setCustomLayout(buildCustomLayout())
                .build()
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            super.onPostConnect(session, controller)
            session.setCustomLayout(controller, buildCustomLayout())
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_TOGGLE_FAVORITE) {
                val currentMediaItem = player.currentMediaItem
                if (currentMediaItem != null) {
                    val station = MediaItemConverter.mediaItemToStation(currentMediaItem)
                    serviceScope.launch(Dispatchers.IO) {
                        RadioApp.instance.repository.toggleFavorite(station)
                        playlistCache.remove(MediaItemConverter.TAB_FAVORITES)
                        playlistCache.remove(MediaItemConverter.TAB_SUGGESTED)
                        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_FAVORITES, 100, null)
                        mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_SUGGESTED, 25, null)
                        withContext(Dispatchers.Main) {
                            updateCustomLayoutGlobally()
                            val isNowFav = cachedFavoriteIds.contains(station.id)
                            android.widget.Toast.makeText(
                                applicationContext,
                                if (isNowFav) "★ Added to Favorites" else "Removed from Favorites",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            } else if (customCommand.customAction == ACTION_TOGGLE_SHUFFLE) {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                val isShuffled = player.shuffleModeEnabled
                updateCustomLayoutGlobally()
                serviceScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        applicationContext,
                        if (isShuffled) "🔀 Shuffle ON" else "Shuffle OFF",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            } else if (customCommand.customAction == ACTION_TOGGLE_HIDE) {
                val currentMediaItem = player.currentMediaItem
                if (currentMediaItem != null) {
                    val station = MediaItemConverter.mediaItemToStation(currentMediaItem)
                    val (_, pureStationId) = MediaItemConverter.parseMediaId(currentMediaItem.mediaId)
                    serviceScope.launch(Dispatchers.IO) {
                        val isNowHidden = RadioApp.instance.repository.toggleHidden(station)
                        withContext(Dispatchers.Main) {
                            if (isNowHidden) {
                                cachedHiddenIds.add(pureStationId)
                                removeStationFromPlayerQueue(pureStationId)
                            } else {
                                cachedHiddenIds.remove(pureStationId)
                            }
                            invalidateAllBrowseCaches()
                            updateCustomLayoutGlobally()
                            android.widget.Toast.makeText(
                                applicationContext,
                                if (isNowHidden) "Station hidden" else "Restored from Hidden",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val isSuggested = params?.isSuggested == true ||
                params?.extras?.getBoolean("android.service.media.extra.SUGGESTED") == true

            DiagnosticLogger.log(
                "RadioMediaService",
                "onGetLibraryRoot() browser='${browser.packageName}', isSuggested=$isSuggested"
            )

            val rootExtras = Bundle().apply {
                putBoolean(MediaItemConverter.CONTENT_STYLE_SUPPORTED, true)
                putInt(MediaItemConverter.CONTENT_STYLE_BROWSABLE_HINT, MediaItemConverter.CONTENT_STYLE_LIST)
                putInt(MediaItemConverter.CONTENT_STYLE_PLAYABLE_HINT, MediaItemConverter.CONTENT_STYLE_GRID)
            }

            if (isSuggested) {
                rootExtras.putBoolean("android.service.media.extra.SUGGESTED", true)
                val libraryParams = LibraryParams.Builder()
                    .setExtras(rootExtras)
                    .setSuggested(true)
                    .build()
                return Futures.immediateFuture(
                    LibraryResult.ofItem(MediaItemConverter.buildSuggestedRootItem(), libraryParams)
                )
            }

            val libraryParams = LibraryParams.Builder()
                .setExtras(rootExtras)
                .build()

            return Futures.immediateFuture(
                LibraryResult.ofItem(MediaItemConverter.buildRootItem(), libraryParams)
            )
        }

        private suspend fun getOrLoadPlaylist(parentId: String): List<MediaItem> {
            val cached = playlistCache[parentId]
            if (!cached.isNullOrEmpty()) {
                return cached
            }

            val hiddenIds = RadioApp.instance.repository.getHiddenStationIdsSync()
                .union(cachedHiddenIds)

            val items: List<MediaItem> = when {
                parentId == MediaItemConverter.TAB_HIDDEN -> {
                    RadioApp.instance.repository.getHiddenStationsSync()
                        .map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_HIDDEN) }
                }
                parentId == MediaItemConverter.TAB_SUGGESTED || parentId == "SUGGESTED" || parentId == "android.media.extra.SUGGESTED" -> {
                    // Instant For You blend (<10ms from local Room DB + memory cache, zero blocking network calls during car startup!)
                    val favorites = RadioApp.instance.repository.getFavoriteStationsSync()
                    val recents = RadioApp.instance.repository.getRecentStationsSync(15)
                    val cachedLocalStations = playlistCache[MediaItemConverter.TAB_LOCAL]
                        ?.map { MediaItemConverter.mediaItemToStation(it) }
                    val local = if (!cachedLocalStations.isNullOrEmpty()) {
                        cachedLocalStations
                    } else {
                        // Trigger non-blocking background warm-up of local stations for future refreshes
                        serviceScope.launch(Dispatchers.IO) {
                            try {
                                val countryCode = com.worldradio.auto.util.CountryDetector.detectCountryCode(this@RadioMediaService)
                                val localResult = RadioApp.instance.repository.getLocalStations(countryCode)
                                val fetched = localResult.getOrNull()
                                if (!fetched.isNullOrEmpty()) {
                                    playlistCache[MediaItemConverter.TAB_LOCAL] = fetched.map {
                                        MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_LOCAL)
                                    }
                                    playlistCache.remove(MediaItemConverter.TAB_SUGGESTED)
                                    mediaLibrarySession?.notifyChildrenChanged(MediaItemConverter.TAB_SUGGESTED, 25, null)
                                }
                            } catch (_: Exception) {}
                        }
                        CuratedStations.ROMANIA_STARTER_STATIONS
                    }

                    val combined = PlaylistQueueHelper.buildSuggestedStations(
                        favorites = favorites,
                        recents = recents,
                        localOrFallback = local,
                        hiddenIds = hiddenIds,
                        limit = 25
                    )

                    combined.map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_SUGGESTED) }
                }
                parentId == MediaItemConverter.TAB_RECENT -> {
                    val recents = RadioApp.instance.repository.getRecentStationsSync(30)
                    val stations = if (recents.isNotEmpty()) {
                        recents
                    } else {
                        PlaylistQueueHelper.filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                    }
                    stations.map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_RECENT) }
                }
                parentId == MediaItemConverter.TAB_FAVORITES -> {
                    val favorites = RadioApp.instance.repository.getFavoriteStationsSync()
                    val stations = if (favorites.isNotEmpty()) {
                        favorites
                    } else {
                        PlaylistQueueHelper.filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                    }
                    stations.sortedBy { it.name.lowercase() }.map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_FAVORITES) }
                }
                parentId == MediaItemConverter.TAB_LOCAL -> {
                    val countryCode = com.worldradio.auto.util.CountryDetector.detectCountryCode(this@RadioMediaService)
                    val localResult = RadioApp.instance.repository.getLocalStations(countryCode)
                    val local = localResult.getOrNull()
                    val stations = if (!local.isNullOrEmpty()) {
                        local
                    } else {
                        PlaylistQueueHelper.filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                    }
                    stations.map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_LOCAL) }
                }
                parentId == MediaItemConverter.TAB_POPULAR -> {
                    val popularResult = RadioApp.instance.repository.getPopularStations()
                    val popular = popularResult.getOrNull()
                    val stations = if (!popular.isNullOrEmpty()) {
                        popular
                    } else {
                        PlaylistQueueHelper.filterStationsForQueue(CuratedStations.GLOBAL_POPULAR_STATIONS, hiddenIds)
                    }
                    stations.map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_POPULAR) }
                }
                parentId.startsWith(MediaItemConverter.PREFIX_COUNTRY) -> {
                    val countryCode = parentId.removePrefix(MediaItemConverter.PREFIX_COUNTRY)
                    val stationsResult = RadioApp.instance.repository.getStationsByCountry(countryCode)
                    val stations = stationsResult.getOrNull()
                    val list = if (!stations.isNullOrEmpty()) {
                        stations
                    } else if (countryCode.equals("RO", ignoreCase = true)) {
                        PlaylistQueueHelper.filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                    } else {
                        emptyList()
                    }
                    list.map { MediaItemConverter.stationToMediaItem(it, parentId) }
                }
                parentId == "search" -> {
                    PlaylistQueueHelper.filterMediaItemsForQueue(lastSearchResults, hiddenIds)
                }
                else -> emptyList()
            }

            if (items.isNotEmpty()) {
                playlistCache[parentId] = items
            }
            return items
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            DiagnosticLogger.log(
                "RadioMediaService",
                "onGetChildren() parentId='$parentId' from browser='${browser.packageName}'"
            )
            // Root tabs returned immediately and synchronously without IPC delay
            if (parentId == MediaItemConverter.ROOT_ID || parentId == "/" || parentId.isEmpty()) {
                val isSuggested = params?.isSuggested == true ||
                    params?.extras?.getBoolean("android.service.media.extra.SUGGESTED") == true

                if (isSuggested) {
                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        try {
                            val items = getOrLoadPlaylist(MediaItemConverter.TAB_SUGGESTED)
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        } catch (e: Exception) {
                            val hiddenIds = cachedHiddenIds.toSet()
                            val fallback = PlaylistQueueHelper
                                .filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                                .map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_SUGGESTED) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(fallback), params))
                        }
                    }
                    return future
                }

                val tabs = MediaItemConverter.buildTopLevelTabs()
                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(tabs), params)
                )
            }

            if (parentId == MediaItemConverter.TAB_WORLD) {
                val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch {
                    try {
                        val countriesResult = RadioApp.instance.repository.getCountries()
                        val countries = countriesResult.getOrNull() ?: CuratedStations.FALLBACK_COUNTRIES
                        val list = countries.sortedBy { it.name.lowercase() }.map { MediaItemConverter.countryToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(list), params))
                    } catch (e: Exception) {
                        val fallback = CuratedStations.FALLBACK_COUNTRIES.map { MediaItemConverter.countryToMediaItem(it) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(fallback), params))
                    }
                }
                return future
            }

            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val items = getOrLoadPlaylist(parentId)
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                } catch (e: Exception) {
                    if (parentId == MediaItemConverter.TAB_HIDDEN) {
                        future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                    } else {
                        val hiddenIds = cachedHiddenIds.toSet()
                        val fallback = PlaylistQueueHelper
                            .filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                            .map { MediaItemConverter.stationToMediaItem(it, parentId) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(fallback), params))
                    }
                }
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            if (mediaId == MediaItemConverter.ROOT_ID) {
                return Futures.immediateFuture(LibraryResult.ofItem(MediaItemConverter.buildRootItem(), null))
            }
            val tab = MediaItemConverter.buildTopLevelTabs().firstOrNull { it.mediaId == mediaId }
            if (tab != null) {
                return Futures.immediateFuture(LibraryResult.ofItem(tab, null))
            }
            val (parentId, pureId) = MediaItemConverter.parseMediaId(mediaId)
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                val dbStation = RadioApp.instance.repository.getStationById(pureId)
                val station = dbStation
                    ?: CuratedStations.ROMANIA_STARTER_STATIONS.firstOrNull { it.id == pureId }
                    ?: CuratedStations.GLOBAL_POPULAR_STATIONS.firstOrNull { it.id == pureId }
                if (station != null) {
                    future.set(LibraryResult.ofItem(MediaItemConverter.stationToMediaItem(station, parentId), null))
                } else {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val results = RadioApp.instance.repository.searchStations(query).getOrDefault(emptyList())
                session.notifySearchResultChanged(browser, query, results.size, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                val stations = RadioApp.instance.repository.searchStations(query).getOrDefault(emptyList())
                val items = stations.map { MediaItemConverter.stationToMediaItem(it, "search") }
                playlistCache["search"] = items
                lastSearchResults = items
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        private suspend fun resolveMediaItem(item: MediaItem): MediaItem {
            val (parentId, pureId) = MediaItemConverter.parseMediaId(item.mediaId)
            if (item.requestMetadata.mediaUri != null && item.requestMetadata.mediaUri.toString().isNotBlank()) {
                return item
            }

            val extras = item.mediaMetadata.extras
            val streamUrl = extras?.getString("stream_url")
            if (!streamUrl.isNullOrBlank()) {
                return item.buildUpon().setUri(android.net.Uri.parse(streamUrl)).build()
            }

            val station = CuratedStations.ROMANIA_STARTER_STATIONS.firstOrNull { it.id == pureId }
                ?: CuratedStations.GLOBAL_POPULAR_STATIONS.firstOrNull { it.id == pureId }
                ?: RadioApp.instance.repository.getStationById(pureId)

            return if (station != null) {
                MediaItemConverter.stationToMediaItem(station, parentId)
            } else {
                item
            }
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            serviceScope.launch {
                val resolved = mediaItems.map { resolveMediaItem(it) }.toMutableList()
                future.set(resolved)
            }
            return future
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                val hiddenIds = RadioApp.instance.repository.getHiddenStationIdsSync()
                    .union(cachedHiddenIds)

                // If a single item is passed (typical when Android Auto selects an item from a browse folder):
                if (mediaItems.size == 1) {
                    val targetItem = mediaItems[0]
                    val (extractedParentId, pureTargetId) = MediaItemConverter.parseMediaId(targetItem.mediaId)
                    val parentFromExtra = targetItem.mediaMetadata.extras?.getString("parent_id")
                    val effectiveParent = extractedParentId?.ifBlank { null } ?: parentFromExtra?.ifBlank { null }
                    val isHiddenTabPlayback = effectiveParent == MediaItemConverter.TAB_HIDDEN

                    var matchedPlaylist: List<MediaItem>? = null

                    // 1. Primary: Use parentId encoded in mediaId (e.g. "[TAB_LOCAL]||uuid")
                    if (!extractedParentId.isNullOrBlank()) {
                        matchedPlaylist = getOrLoadPlaylist(extractedParentId)
                    }

                    // 2. Fallback: Check parent_id in extras
                    if (matchedPlaylist.isNullOrEmpty() && !parentFromExtra.isNullOrBlank()) {
                        matchedPlaylist = getOrLoadPlaylist(parentFromExtra)
                    }

                    // 3. Fallback: Search active non-hidden playlist caches for this station ID
                    if (matchedPlaylist.isNullOrEmpty()) {
                        matchedPlaylist = playlistCache.entries
                            .firstOrNull { (key, list) ->
                                (isHiddenTabPlayback || key != MediaItemConverter.TAB_HIDDEN) &&
                                    list.any { MediaItemConverter.parseMediaId(it.mediaId).second == pureTargetId }
                            }?.value
                    }

                    if (!matchedPlaylist.isNullOrEmpty()) {
                        val filteredPlaylist = PlaylistQueueHelper.filterMediaItemsForQueue(
                            matchedPlaylist,
                            hiddenIds,
                            allowHidden = isHiddenTabPlayback
                        )
                        if (filteredPlaylist.isNotEmpty()) {
                            val targetIndex = filteredPlaylist.indexOfFirst {
                                MediaItemConverter.parseMediaId(it.mediaId).second == pureTargetId
                            }.coerceAtLeast(0)

                            val resolvedPlaylist = filteredPlaylist.map { resolveMediaItem(it) }
                            future.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    resolvedPlaylist,
                                    targetIndex,
                                    startPositionMs
                                )
                            )
                            return@launch
                        }
                    }
                }

                // If multiple items are passed (from phone UI) or fallback
                val allowHidden = mediaItems.isNotEmpty() && mediaItems.all {
                    MediaItemConverter.parseMediaId(it.mediaId).first == MediaItemConverter.TAB_HIDDEN ||
                        it.mediaMetadata.extras?.getBoolean("is_hidden", false) == true
                }
                val filtered = PlaylistQueueHelper.filterMediaItemsForQueue(mediaItems, hiddenIds, allowHidden)
                val resolved = filtered.map { resolveMediaItem(it) }
                val safeIndex = if (resolved.isEmpty()) 0 else startIndex.coerceIn(0, resolved.size - 1)
                future.set(MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs))
            }
            return future
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

            serviceScope.launch {
                val hiddenIds = RadioApp.instance.repository.getHiddenStationIdsSync()
                    .union(cachedHiddenIds)
                val recents = RadioApp.instance.repository.getRecentStationsSync(30)
                val items = if (recents.isNotEmpty()) {
                    recents.map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_RECENT) }
                } else {
                    PlaylistQueueHelper.filterStationsForQueue(CuratedStations.ROMANIA_STARTER_STATIONS, hiddenIds)
                        .map { MediaItemConverter.stationToMediaItem(it, MediaItemConverter.TAB_RECENT) }
                }
                playlistCache[MediaItemConverter.TAB_RECENT] = items
                val resolved = items.map { resolveMediaItem(it) }
                future.set(
                    MediaSession.MediaItemsWithStartPosition(
                        resolved,
                        /* startIndex = */ 0,
                        /* startPositionMs = */ C.TIME_UNSET
                    )
                )
            }
            return future
        }
    }

    override fun onDestroy() {
        DiagnosticLogger.log("RadioMediaService", "onDestroy() service stopping")
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
