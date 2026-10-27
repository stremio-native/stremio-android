package com.stremio.mobile.core

import android.content.Context
import timber.log.Timber
import com.stremio.core.Core
import com.stremio.core.Field
import com.stremio.core.models.Ctx
import com.stremio.core.models.LoadableConvertedStream
import com.stremio.core.models.LoadableMetaItem
import com.stremio.core.models.LoadableStreams
import com.stremio.core.models.MetaDetails
import com.stremio.core.models.Player
import com.stremio.core.models.CatalogsWithExtra
import com.stremio.core.models.CatalogWithFilters
import com.stremio.core.models.ContinueWatchingPreview
import com.stremio.core.models.LoadablePage
import com.stremio.core.models.LibraryWithFilters
import com.stremio.mobile.data.model.CatalogItem
import com.stremio.core.types.resource.Video
import com.stremio.core.runtime.RuntimeEvent
import com.stremio.core.runtime.msg.Action
import com.stremio.core.runtime.msg.ActionCtx
import com.stremio.core.runtime.msg.ActionLoad
import com.stremio.core.runtime.msg.ActionPlayer
import com.stremio.core.types.addon.ExtraValue
import com.stremio.core.types.addon.ResourcePath
import com.stremio.core.types.addon.ResourceRequest
import com.stremio.core.types.api.AuthRequest
import com.stremio.core.types.profile.GDPRConsent
import com.stremio.core.types.resource.Stream
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * One stream option surfaced for a meta item, paired with the addon request it came from so it
 * can later be converted into a playable URL by the core.
 */
data class CoreStream(
    val stream: Stream,
    val streamRequest: ResourceRequest,
    val metaRequest: ResourceRequest?,
    val addonTitle: String,
)

/** The user's global subtitle preferences, persisted on the core profile. */
data class SubtitlePrefs(
    val language: String?,
    val sizePercent: Int,
    val offsetPercent: Int,
)

/**
 * Thin wrapper around the stremio-core JNI bridge. It owns core initialisation, exposes the
 * runtime event stream as a [SharedFlow], and drives the MetaDetails -> streams and
 * Player -> resolved-URL models used for playback.
 */
class StremioCore(context: Context) {
    private val tag = "StremioCore"
    private val storage = AndroidStorage(context)

    private val _events = MutableSharedFlow<RuntimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RuntimeEvent> = _events.asSharedFlow()

    private val listener = Core.EventListener { event -> _events.tryEmit(event) }

    var initialized = false
        private set

    fun initialize() {
        if (initialized) return
        Core.addEventListener(listener)
        val error = runCatching { Core.initialize(storage) }.getOrElse { e ->
            Timber.tag(tag).e(e, "Core.initialize threw")
            null
        }
        if (error != null) {
            Timber.tag(tag).e("Core.initialize failed: code=%d message=%s", error.code, error.message)
        }
        initialized = true
    }

    // region auth -------------------------------------------------------------------------------

    fun authenticate(request: AuthRequest) {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.Authenticate(request)))), Field.CTX)
    }

    fun loginWithToken(token: String) {
        authenticate(AuthRequest(AuthRequest.Type.LoginWithToken(AuthRequest.LoginWithToken(token))))
    }

    fun loginWithFacebook(token: String) {
        authenticate(AuthRequest(AuthRequest.Type.Facebook(AuthRequest.Facebook(token))))
    }

    fun login(email: String, password: String) {
        authenticate(
            AuthRequest(AuthRequest.Type.Login(AuthRequest.Login(email = email, password = password, facebook = false))),
        )
    }

    fun register(email: String, password: String, marketing: Boolean) {
        val consent = GDPRConsent(tos = true, privacy = true, marketing = marketing, from = "stremio-mobile")
        authenticate(
            AuthRequest(AuthRequest.Type.Register(AuthRequest.Register(email = email, password = password, gdprConsent = consent))),
        )
    }

    fun logout() {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.Logout(pbandk.wkt.Empty())))), Field.CTX)
    }

    fun logoutTrakt() {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.LogoutTrakt(pbandk.wkt.Empty())))), Field.CTX)
    }

    fun installTraktAddon() {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.InstallTraktAddon(pbandk.wkt.Empty())))), Field.CTX)
    }

    fun getCtx(): Ctx = Core.getState(Field.CTX)

    /** Whether the core currently holds an authenticated profile (the user is logged in). */
    fun isAuthenticated(): Boolean = runCatching { getCtx().profile.auth != null }.getOrDefault(false)

    /** The user's global subtitle preferences (matches web's persisted Profile.Settings). */
    fun getSubtitleSettings(): SubtitlePrefs {
        val settings = getCtx().profile.settings
        return SubtitlePrefs(
            language = settings.subtitlesLanguage,
            sizePercent = settings.subtitlesSize,
            offsetPercent = settings.subtitlesOffset,
        )
    }

    /** Persists subtitle size/offset/language to the core profile, like web's Settings screen. */
    fun updateSubtitleSettings(sizePercent: Int? = null, offsetPercent: Int? = null, language: String? = null) {
        val current = getCtx().profile.settings
        val updated = current.copy(
            subtitlesSize = sizePercent ?: current.subtitlesSize,
            subtitlesOffset = offsetPercent ?: current.subtitlesOffset,
            subtitlesLanguage = language ?: current.subtitlesLanguage,
        )
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.UpdateSettings(updated)))), Field.CTX)
    }

    fun updateSettings(settings: com.stremio.core.types.profile.Profile.Settings) {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.UpdateSettings(settings)))), Field.CTX)
    }

    fun updateStreamingServerSettings(settings: com.stremio.core.models.StreamingServer.Settings) {
        dispatch(
            Action(Action.Type.StreamingServer(com.stremio.core.runtime.msg.ActionStreamingServer(com.stremio.core.runtime.msg.ActionStreamingServer.Args.UpdateSettings(settings)))),
            Field.STREAMING_SERVER
        )
    }

    fun requestStreamStatistics(infoHash: String, fileIndex: Int) {
        val request = com.stremio.core.models.StreamingServer.StatisticsRequest(infoHash = infoHash, fileIndex = fileIndex)
        dispatch(
            Action(Action.Type.StreamingServer(com.stremio.core.runtime.msg.ActionStreamingServer(com.stremio.core.runtime.msg.ActionStreamingServer.Args.GetStatistics(request)))),
            Field.STREAMING_SERVER
        )
    }

    /** The most recently fetched live torrent statistics, or null until [requestStreamStatistics] resolves. */
    fun getStreamStatistics(): com.stremio.core.models.StreamingServer.Statistics? {
        val content = getStreamingServer().statistics?.content
        return (content as? com.stremio.core.models.LoadableStatistics.Content.Ready)?.value
    }

    fun getStreamingServer(): com.stremio.core.models.StreamingServer = Core.getState(Field.STREAMING_SERVER)

    fun streamingServerFlow(): kotlinx.coroutines.flow.Flow<com.stremio.core.models.StreamingServer> = newStateFlow(Field.STREAMING_SERVER)
        .map { getStreamingServer() }
        .onStart { emit(getStreamingServer()) }

    fun ctx(): Flow<Ctx> = newStateFlow(Field.CTX)
        .map { getCtx() }
        .onStart { emit(getCtx()) }

    // endregion

    // region board and discover -----------------------------------------------------------------

    fun getBoard(): CatalogsWithExtra = Core.getState(Field.BOARD)

    fun board(): Flow<CatalogsWithExtra> = newStateFlow(Field.BOARD)
        .map { getBoard() }
        .onStart {
            loadBoard()
            emit(getBoard())
        }

    fun loadBoard() {
        val selected = CatalogsWithExtra.Selected(type = null, extra = emptyList())
        dispatchLoad(ActionLoad.Args.CatalogsWithExtra(selected), Field.BOARD)
    }

    fun loadBoardRange(start: Int, end: Int) {
        val range = com.stremio.core.runtime.msg.Range(start = start, end = end)
        val args = com.stremio.core.runtime.msg.ActionCatalogsWithExtra.Args.LoadRange(range)
        val action = Action(Action.Type.CatalogsWithExtra(com.stremio.core.runtime.msg.ActionCatalogsWithExtra(args)))
        dispatch(action, Field.BOARD)
    }


    fun getContinueWatchingPreview(): ContinueWatchingPreview = Core.getState(Field.CONTINUE_WATCHING_PREVIEW)

    fun continueWatchingPreview(): Flow<ContinueWatchingPreview> = newStateFlow(Field.CONTINUE_WATCHING_PREVIEW)
        .map { getContinueWatchingPreview() }
        .onStart { emit(getContinueWatchingPreview()) }

    fun getDiscover(): CatalogWithFilters = Core.getState(Field.DISCOVER)

    fun discover(): Flow<CatalogWithFilters> = newStateFlow(Field.DISCOVER)
        .map { getDiscover() }
        .onStart { emit(getDiscover()) }

    fun loadDiscover(request: ResourceRequest) {
        val selected = CatalogWithFilters.Selected(request = request)
        dispatchLoad(ActionLoad.Args.CatalogWithFilters(selected), Field.DISCOVER)
    }

    // endregion

    // region search -----------------------------------------------------------------------------

    fun getSearch(): CatalogsWithExtra = Core.getState(Field.SEARCH)

    /** Search every installed catalog addon for [query] (a CatalogsWithExtra with a `search` extra). */
    fun search(query: String): Flow<CatalogsWithExtra> = newStateFlow(Field.SEARCH)
        .map { getSearch() }
        .onStart {
            loadSearch(query)
            emit(getSearch())
        }

    fun loadSearch(query: String) {
        val selected = CatalogsWithExtra.Selected(
            type = null,
            extra = listOf(ExtraValue(name = "search", value = query)),
        )
        dispatchLoad(ActionLoad.Args.Search(selected), Field.SEARCH)
    }

    fun loadSearchRange(start: Int, end: Int) {
        val range = com.stremio.core.runtime.msg.Range(start = start, end = end)
        val args = com.stremio.core.runtime.msg.ActionCatalogsWithExtra.Args.LoadRange(range)
        dispatch(Action(Action.Type.CatalogsWithExtra(com.stremio.core.runtime.msg.ActionCatalogsWithExtra(args))), Field.SEARCH)
    }

    // endregion

    // region library ----------------------------------------------------------------------------

    fun getLibrary(): LibraryWithFilters = Core.getState(Field.LIBRARY)

    fun library(): Flow<LibraryWithFilters> = newStateFlow(Field.LIBRARY)
        .map { getLibrary() }
        .onStart {
            val current = getLibrary()
            if (current.selected == null) {
                val defaultRequest = LibraryWithFilters.LibraryRequest(
                    type = null,
                    sort = LibraryWithFilters.Sort.LAST_WATCHED,
                    page = 1L
                )
                loadLibrary(defaultRequest)
            }
            emit(getLibrary())
        }

    fun loadLibrary(request: LibraryWithFilters.LibraryRequest) {
        val selected = LibraryWithFilters.Selected(request = request)
        dispatchLoad(ActionLoad.Args.LibraryWithFilters(selected), Field.LIBRARY)
    }

    fun addToLibrary(item: CatalogItem) {
        val preview = com.stremio.core.types.resource.MetaItemPreview(
            id = item.id,
            type = item.type,
            name = item.name,
            posterShape = com.stremio.core.types.resource.PosterShape.POSTER,
            poster = item.poster,
            background = item.background,
            logo = null,
            description = null,
            releaseInfo = item.releaseInfo,
            runtime = null,
            released = null,
            links = emptyList(),
            behaviorHints = com.stremio.core.types.resource.MetaItemBehaviorHints(hasScheduledVideos = false),
            deepLinks = com.stremio.core.types.resource.MetaItemDeepLinks(),
            inLibrary = true,
            watched = false,
            inCinema = false
        )
        val action = Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.AddToLibrary(preview))))
        dispatch(action, Field.CTX)
    }

    fun removeFromLibrary(id: String) {
        val action = Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.RemoveFromLibrary(id))))
        dispatch(action, Field.CTX)
    }

    fun syncLibrary() {
        val action = Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.SyncLibraryWithApi(pbandk.wkt.Empty()))))
        dispatch(action, Field.CTX)
    }

    // endregion

    // region addons -------------------------------------------------------------------------------

    fun getAddons(): com.stremio.core.models.AddonsWithFilters = Core.getState(Field.ADDONS)

    fun addons(): Flow<com.stremio.core.models.AddonsWithFilters> = newStateFlow(Field.ADDONS)
        .map { getAddons() }
        .onStart {
            if (getAddons().selected == null) {
                loadInstalledAddons()
            }
            emit(getAddons())
        }

    fun loadAddons(request: ResourceRequest) {
        val selected = com.stremio.core.models.AddonsWithFilters.Selected(request = request)
        dispatchLoad(ActionLoad.Args.AddonsWithFilters(selected), Field.ADDONS)
    }

    /** Loads the user's installed addons, optionally filtered by a manifest-supported [type]. */
    fun loadInstalledAddons(type: String? = null) {
        val request = ResourceRequest(
            base = "",
            path = ResourcePath(resource = "", type = type ?: "", id = ""),
        )
        loadAddons(request)
    }

    fun installAddon(descriptor: com.stremio.core.types.addon.AddonDescriptor) {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.InstallAddon(descriptor)))), Field.CTX)
    }

    fun uninstallAddon(descriptor: com.stremio.core.types.addon.AddonDescriptor) {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.UninstallAddon(descriptor)))), Field.CTX)
    }

    fun upgradeAddon(descriptor: com.stremio.core.types.addon.AddonDescriptor) {
        dispatch(Action(Action.Type.Ctx(ActionCtx(ActionCtx.Args.UpgradeAddon(descriptor)))), Field.CTX)
    }

    fun getAddonDetails(): com.stremio.core.models.AddonDetails = Core.getState(Field.ADDON_DETAILS)

    fun addonDetails(transportUrl: String): Flow<com.stremio.core.models.AddonDetails> =
        newStateFlow(Field.ADDON_DETAILS)
            .map { getAddonDetails() }
            .onStart {
                loadAddonDetails(transportUrl)
                emit(getAddonDetails())
            }

    fun loadAddonDetails(transportUrl: String) {
        val selected = com.stremio.core.models.AddonDetails.Selected(transportUrl = transportUrl)
        dispatchLoad(ActionLoad.Args.AddonDetails(selected), Field.ADDON_DETAILS)
    }

    // endregion

    // region streams ----------------------------------------------------------------------------

    /**
     * Emits the latest [MetaDetails] each time the core publishes a new state for it. Loading is
     * triggered on collection, and streams arrive incrementally as each installed addon responds.
     *
     * @param videoId when set, streams are loaded for that specific video (series episode).
     * @param guessStreamPath when true (movies), the core auto-selects the stream path so streams
     *   load without an explicit [videoId].
     */
    fun metaDetails(type: String, id: String, videoId: String?, guessStreamPath: Boolean): Flow<MetaDetails> {
        val metaPath = ResourcePath(resource = "meta", type = type, id = id)
        val streamPath = videoId?.let { ResourcePath(resource = "stream", type = type, id = it) }
        val selected = MetaDetails.Selected(
            metaPath = metaPath,
            streamPath = streamPath,
            guessStreamPath = guessStreamPath,
        )
        return newStateFlow(Field.META_DETAILS)
            .map { getMetaDetails() }
            .onStart {
                dispatchLoad(ActionLoad.Args.MetaDetails(selected), Field.META_DETAILS)
                emit(getMetaDetails())
            }
    }

    fun getMetaDetails(): MetaDetails = Core.getState(Field.META_DETAILS)

    /** The episodes/videos of the loaded meta item, once it is ready (empty otherwise). */
    fun extractVideos(details: MetaDetails): List<Video> {
        val content = details.metaItem?.content
        return if (content is LoadableMetaItem.Content.Ready) content.value.videos else emptyList()
    }

    /** Flattens every addon's ready streams into [CoreStream]s, tagging each with its origin. */
    fun extractStreams(details: MetaDetails): List<CoreStream> {
        val metaRequest = details.metaItem?.request
        return details.streams.flatMap { loadable: LoadableStreams ->
            val content = loadable.content
            if (content is LoadableStreams.Content.Ready) {
                content.value.streams.map { stream ->
                    CoreStream(
                        stream = stream,
                        streamRequest = loadable.request,
                        metaRequest = metaRequest,
                        addonTitle = loadable.title,
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    // endregion

    // region playback resolution ----------------------------------------------------------------

    /**
     * Loads the Player model for [option] and emits the resolved playable URL once the core has
     * converted the stream (torrents are routed through the local streaming server). Direct URL
     * streams are emitted immediately as a fallback.
     */
    fun resolvePlayableUrl(option: CoreStream): Flow<String> {
        val streamPath = option.streamRequest.path
        val selected = Player.Selected(
            stream = option.stream,
            streamRequest = option.streamRequest,
            metaRequest = option.metaRequest,
            // Without a subtitles path the core never asks addons (e.g. OpenSubtitles) for
            // subtitles, so only tracks embedded in the video or bundled in the stream show up.
            subtitlesPath = ResourcePath(resource = "subtitles", type = streamPath.type, id = streamPath.id),
        )
        return newStateFlow(Field.PLAYER)
            .map { directUrl(getPlayer().stream) }
            .onStart {
                dispatchLoad(ActionLoad.Args.Player(selected), Field.PLAYER)
                // Direct URL streams need no conversion.
                directUrl(option.stream)?.let { emit(it) }
            }
            .filter { it != null }
            .map { it!! }
    }

    fun getPlayer(): Player = Core.getState(Field.PLAYER)

    fun playerFlow(): Flow<Player> = newStateFlow(Field.PLAYER)
        .map { getPlayer() }
        .onStart { emit(getPlayer()) }

    fun updatePlayerStreamState(transform: (Player.StreamState) -> Player.StreamState) {
        val current = getPlayer().streamState ?: Player.StreamState()
        playerStreamStateChanged(transform(current))
    }

    fun playerStreamStateChanged(streamState: Player.StreamState) {
        dispatch(Action(Action.Type.Player(ActionPlayer(ActionPlayer.Args.StreamStateChanged(streamState)))), Field.PLAYER)
    }

    fun setPlayerAudioTrack(id: String, language: String?) {
        updatePlayerStreamState { current ->
            current.copy(audioTrack = Player.AudioTrack(id = id, language = language))
        }
    }

    fun setPlayerSubtitleTrack(id: String, embedded: Boolean, language: String?) {
        updatePlayerStreamState { current ->
            current.copy(subtitleTrack = Player.SubtitleTrack(id = id, embedded = embedded, language = language))
        }
    }

    fun clearPlayerSubtitleTrack() {
        updatePlayerStreamState { current ->
            current.copy(subtitleTrack = null)
        }
    }

    fun setPlayerSubtitleSettings(delayMs: Long? = null, sizePercent: Float? = null, offsetPercent: Float? = null) {
        updatePlayerStreamState { current ->
            current.copy(
                subtitleDelay = delayMs ?: current.subtitleDelay,
                subtitleSize = sizePercent ?: current.subtitleSize,
                subtitleOffset = offsetPercent ?: current.subtitleOffset,
            )
        }
    }

    /** The next episode's metadata, if the core found one in the series' video list (null for movies / last episode). */
    fun getNextVideo(): Video? = getPlayer().nextVideo

    /**
     * The saved watch position for [streamRequest]'s video, or 0 if there's no saved progress or
     * the saved progress belongs to a different video (e.g. a different episode).
     */
    fun getResumePositionMs(streamRequest: ResourceRequest?): Long {
        val state = getPlayer().libraryItem?.state ?: return 0L
        return if (state.videoId == streamRequest?.path?.id) state.timeOffset else 0L
    }

    fun playerTimeChanged(timeMs: Long, durationMs: Long) {
        val state = ActionPlayer.PlayerItemState(time = timeMs, duration = durationMs, device = DEVICE_NAME)
        dispatch(Action(Action.Type.Player(ActionPlayer(ActionPlayer.Args.TimeChanged(state)))), Field.PLAYER)
    }

    fun playerSeek(timeMs: Long, durationMs: Long) {
        val state = ActionPlayer.PlayerItemState(time = timeMs, duration = durationMs, device = DEVICE_NAME)
        dispatch(Action(Action.Type.Player(ActionPlayer(ActionPlayer.Args.SeekAction(state)))), Field.PLAYER)
    }

    fun playerPausedChanged(paused: Boolean) {
        dispatch(Action(Action.Type.Player(ActionPlayer(ActionPlayer.Args.PausedChanged(paused)))), Field.PLAYER)
    }

    fun playerEnded() {
        dispatch(Action(Action.Type.Player(ActionPlayer(ActionPlayer.Args.Ended(pbandk.wkt.Empty())))), Field.PLAYER)
    }

    fun playerNextVideo() {
        dispatch(Action(Action.Type.Player(ActionPlayer(ActionPlayer.Args.NextVideo(pbandk.wkt.Empty())))), Field.PLAYER)
    }

    private fun directUrl(converted: LoadableConvertedStream?): String? {
        val content = converted?.content
        return if (content is LoadableConvertedStream.Content.Ready) directUrl(content.value) else null
    }

    /** Best-effort extraction of an ExoPlayer-playable URL from a core [Stream]. */
    fun directUrl(stream: Stream): String? {
        return when (val source = stream.source) {
            is Stream.Source.Url -> source.value.url
            is Stream.Source.Tramvai -> {
                val t = source.value
                "$STREAMING_SERVER_BASE/${t.infoHash}/${t.fileIdx ?: STREAMING_SERVER_AUTO_FILE_INDEX}"
            }
            is Stream.Source.External -> source.value.externalUrl
            else -> null
        }
    }

    // endregion

    private fun dispatchLoad(args: ActionLoad.Args<*>, field: Field) {
        dispatch(Action(Action.Type.Load(ActionLoad(args))), field)
    }

    private fun dispatch(action: Action, field: Field) {
        runCatching { Core.dispatch(action, field) }
            .onFailure { Timber.tag(tag).e(it, "dispatch failed for %s", field) }
    }

    private fun newStateFlow(field: Field): Flow<Unit> = callbackFlow {
        val cb = Core.EventListener { event ->
            val inner = event.event
            if (inner is RuntimeEvent.Event.NewState && inner.value.fields.contains(field)) {
                trySend(Unit)
            }
        }
        Core.addEventListener(cb)
        awaitClose { Core.removeEventListener(cb) }
    }

    companion object {
        const val STREAMING_SERVER_BASE = "http://127.0.0.1:11470"
        const val STREAMING_SERVER_AUTO_FILE_INDEX = -1
        private const val DEVICE_NAME = "android"
    }
}
