package com.stremio.mobile.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stremio.core.runtime.RuntimeEvent
import com.stremio.core.runtime.msg.Event
import com.stremio.mobile.core.CoreStream
import com.stremio.mobile.core.StremioCore
import com.stremio.mobile.core.utils.parseStreamDescription
import com.stremio.mobile.data.model.*
import com.stremio.mobile.data.repository.*
import com.stremio.mobile.player.PlaybackState
import com.stremio.mobile.player.ExternalSubtitle
import com.stremio.mobile.player.LanguageCatalog
import com.stremio.mobile.player.PlayerEngine
import com.stremio.mobile.player.PlayerSubtitleStyle
import com.stremio.mobile.player.PlayerTrackOption
import com.stremio.mobile.core.theme.AppFont
import timber.log.Timber
import com.stremio.mobile.presentation.state.*
import com.stremio.mobile.server.StreamingServerController
import com.stremio.mobile.server.StreamingServerState
import com.stremio.mobile.server.formatServerErrorMessage
import com.stremio.mobile.update.ApkInstaller
import com.stremio.mobile.update.UpdateInfo
import com.stremio.mobile.update.UpdateRepository
import com.stremio.mobile.update.UpdateState

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class MainViewModel(
    private val authRepository: AuthRepository,
    private val boardRepository: BoardRepository,
    private val catalogRepository: CatalogRepository,
    private val addonRepository: AddonRepository,
    private val playbackRepository: PlaybackRepository,
    private val updateRepository: UpdateRepository,
    private val apkInstaller: ApkInstaller,
    private val serverController: StreamingServerController,
    private val core: StremioCore,
    appContext: Context,
) : ViewModel() {
    private val appContext = appContext.applicationContext
    private val latestIntentUri = MutableStateFlow<String?>(null)
    private val account = MutableStateFlow(authRepository.accountFromCore())
    private var authInFlight = false
    private val selectedSection = MutableStateFlow(MainSection.Home)
    private val searchQuery = MutableStateFlow("")
    private val searchResults = MutableStateFlow(CatalogShelf(title = "Search", isLoading = false))
    private val searchShelves = MutableStateFlow<List<CatalogShelf>>(emptyList())
    private val isSearchOpen = MutableStateFlow(false)
    private val library = MutableStateFlow(CatalogShelf(title = "Library", isLoading = false))
    private val addons = MutableStateFlow(AddonsUiState())
    private val selectedAddonDetails = MutableStateFlow<AddonDetailsUiState?>(null)
    private val nextVideo = MutableStateFlow<com.stremio.core.types.resource.Video?>(null)
    private val showNextVideoPopup = MutableStateFlow(false)
    private val showNoSeedsBanner = MutableStateFlow(false)
    private val noSeedsReason = MutableStateFlow<String?>(null)
    private val selectedDetails = MutableStateFlow<MetaDetails?>(null)
    private val continueWatching = MutableStateFlow(CatalogShelf(title = "Continue Watching"))
    private val boardShelves = MutableStateFlow<List<CatalogShelf>>(emptyList())
    private val requestedRequests = mutableSetOf<String>()

    @Volatile
    private var cachedBoard: com.stremio.core.models.CatalogsWithExtra? = null

    private val discoverCatalogRequest = MutableStateFlow<com.stremio.core.types.addon.ResourceRequest?>(null)
    private val discoverCatalogTitle = MutableStateFlow<String?>(null)
    private val discoverCatalog = MutableStateFlow(CatalogShelf(title = "Discover", isLoading = false))
    private val discoverCatalogWithFilters = MutableStateFlow<com.stremio.core.models.CatalogWithFilters?>(null)
    private val libraryWithFilters = MutableStateFlow<com.stremio.core.models.LibraryWithFilters?>(null)
    private val isDiscoverSeeAll = MutableStateFlow(false)
    private val profileSettings = MutableStateFlow<com.stremio.core.types.profile.Profile.Settings?>(null)
    private val serverSettings = MutableStateFlow<com.stremio.core.models.StreamingServer.Settings?>(null)
    private val isAutoStartOnBoot = MutableStateFlow(authRepository.isAutoStartOnBoot())
    private val isServerInForeground = MutableStateFlow(authRepository.isServerInForeground())
    private val isMobileDataWarning = MutableStateFlow(authRepository.isMobileDataWarning())
    private val isKeepScreenOn = MutableStateFlow(authRepository.isKeepScreenOn())
    private val isAnalyticsEnabled = MutableStateFlow(authRepository.isAnalyticsEnabled())
    private val showAnalyticsDisclosure = MutableStateFlow(false)
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState
    private val isAutoUpdateEnabled = MutableStateFlow(authRepository.isAutoUpdateEnabled())
    private val isTraktAuthenticated = MutableStateFlow(false)
    private val isSeedingEnabled = MutableStateFlow(true)
    private val minSeedsThreshold = MutableStateFlow(authRepository.getMinSeedsThreshold())
    private val minDownloadSpeedBps = MutableStateFlow(authRepository.getMinDownloadSpeedBps())
    private val preferredQuality = MutableStateFlow(authRepository.getPreferredQuality())
    private val globalUiStyle = MutableStateFlow(authRepository.getGlobalUiStyle())
    private val playerUiStyle = MutableStateFlow(authRepository.getPlayerUiStyle())
    private val glassEffectsMode = MutableStateFlow(authRepository.getGlassEffectsMode())
    private val isAutoSwitchOnDeadStream = MutableStateFlow(authRepository.isAutoSwitchOnDeadStream())
    private val globalGlassAlpha = MutableStateFlow(authRepository.getGlobalGlassAlpha())
    private val adaptiveGlassContrast = MutableStateFlow(authRepository.isAdaptiveGlassContrastEnabled())
    private val glassHapticsEnabled = MutableStateFlow(authRepository.getGlassHapticsEnabled())
    private val hapticsIntensity = MutableStateFlow(authRepository.getHapticsIntensity())
    private val liquidGlassTuning = MutableStateFlow(authRepository.getLiquidGlassTuning())
    private val selectedFont = MutableStateFlow(authRepository.getSelectedFont())
    private val pendingMobileDataStream = MutableStateFlow<StreamOption?>(null)

    private var lastServerActivityMs: Long = System.currentTimeMillis()
    private val IDLE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
    private val UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    private val serverPingStatus = MutableStateFlow("Not checked")
    private val isPingLoading = MutableStateFlow(false)

    private val configPath = File(this.appContext.filesDir, "stream-server").absolutePath
    private val cachePath = File(this.appContext.cacheDir, "stream-server").absolutePath

    private val streams = MutableStateFlow(StreamsUiState())
    val streamsState: StateFlow<StreamsUiState> = streams

    private val isRestoringSession = MutableStateFlow(
        run {
            val authKey = authRepository.getSavedAuthKey()
            !authKey.isNullOrBlank() && authKey != "mock_auth_key" && !authRepository.isAuthenticated()
        }
    )
    val sessionRestoring: StateFlow<Boolean> = isRestoringSession

    val playbackState: StateFlow<PlaybackState> = playbackRepository.state

    private val playerOpen = MutableStateFlow(false)
    val isPlayerOpen: StateFlow<Boolean> = playerOpen

    private var streamsJob: Job? = null
    private var playJob: Job? = null
    private var searchJob: Job? = null
    private var detailsJob: Job? = null
    private var addonDetailsJob: Job? = null
    private var nextVideoJob: Job? = null
    private var subtitleObserverJob: Job? = null
    private var lastPlayedOption: StreamOption? = null
    private var dismissedNextVideoId: String? = null
    private var lastTimeReportMs = 0L
    private var noSeedsPollJob: Job? = null
    private var unhealthySinceMs: Long? = null

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MainUiState> = combine(
        listOf(
            serverController.state,
            serverPingStatus,
            isPingLoading,
            latestIntentUri,
            account,
            selectedSection,
            searchQuery,
            searchResults,
            library,
            addons,
            selectedDetails,
            continueWatching,
            boardShelves,
            discoverCatalogRequest,
            discoverCatalogTitle,
            discoverCatalog,
            discoverCatalogWithFilters,
            libraryWithFilters,
            isDiscoverSeeAll,
            searchShelves,
            profileSettings,
            serverSettings,
            isAutoStartOnBoot,
            isServerInForeground,
            isMobileDataWarning,
            isKeepScreenOn,
            isTraktAuthenticated,
            isSeedingEnabled,
            isSearchOpen,
            selectedAddonDetails,
            nextVideo,
            showNextVideoPopup,
            minSeedsThreshold,
            minDownloadSpeedBps,
            preferredQuality,
            globalUiStyle,
            glassEffectsMode,
            isAutoSwitchOnDeadStream,
            showNoSeedsBanner,
            noSeedsReason,
            globalGlassAlpha,
            adaptiveGlassContrast,
            glassHapticsEnabled,
            hapticsIntensity,
            liquidGlassTuning,
            selectedFont,
            pendingMobileDataStream,
            isAnalyticsEnabled,
            showAnalyticsDisclosure,
            _updateState,
            isAutoUpdateEnabled,
            playerUiStyle,
        )
    ) { values ->
        MainUiState(
            server = values[0] as StreamingServerState,
            selectedFont = values[45] as AppFont,
            showMobileDataWarning = values[46] != null,
            isAnalyticsEnabled = values[47] as Boolean,
            showAnalyticsDisclosure = values[48] as Boolean,
            updateState = values[49] as UpdateState,
            isAutoUpdateEnabled = values[50] as Boolean,
            serverPingStatus = values[1] as String,
            isPingLoading = values[2] as Boolean,
            serverVersion = "0.1.8",
            serverConfigPath = configPath,
            serverCachePath = cachePath,
            latestIntentUri = values[3] as String?,
            account = values[4] as AccountUiState,
            selectedSection = values[5] as MainSection,
            searchQuery = values[6] as String,
            searchResults = values[7] as CatalogShelf,
            searchShelves = values[19] as List<CatalogShelf>,
            library = values[8] as CatalogShelf,
            addons = values[9] as AddonsUiState,
            selectedDetails = values[10] as MetaDetails?,
            continueWatching = values[11] as CatalogShelf,
            boardShelves = values[12] as List<CatalogShelf>,
            discoverCatalogRequest = values[13] as com.stremio.core.types.addon.ResourceRequest?,
            discoverCatalogTitle = values[14] as String?,
            discoverCatalog = values[15] as CatalogShelf,
            discoverCatalogWithFilters = values[16] as com.stremio.core.models.CatalogWithFilters?,
            libraryWithFilters = values[17] as com.stremio.core.models.LibraryWithFilters?,
            isDiscoverSeeAll = values[18] as Boolean,
            profileSettings = values[20] as com.stremio.core.types.profile.Profile.Settings?,
            serverSettings = values[21] as com.stremio.core.models.StreamingServer.Settings?,
            isAutoStartOnBoot = values[22] as Boolean,
            isServerInForeground = values[23] as Boolean,
            isMobileDataWarning = values[24] as Boolean,
            isKeepScreenOn = values[25] as Boolean,
            isTraktAuthenticated = values[26] as Boolean,
            isSeedingEnabled = values[27] as Boolean,
            isSearchOpen = values[28] as Boolean,
            selectedAddonDetails = values[29] as AddonDetailsUiState?,
            nextVideo = values[30] as com.stremio.core.types.resource.Video?,
            showNextVideoPopup = values[31] as Boolean,
            minSeedsThreshold = values[32] as Int,
            minDownloadSpeedBps = values[33] as Long,
            preferredQuality = values[34] as String,
            globalUiStyle = values[35] as String,
            playerUiStyle = values[51] as String,
            glassEffectsMode = values[36] as String,
            isAutoSwitchOnDeadStream = values[37] as Boolean,
            showNoSeedsBanner = values[38] as Boolean,
            noSeedsReason = values[39] as String?,
            globalGlassAlpha = values[40] as Float,
            adaptiveGlassContrast = values[41] as Boolean,
            glassHapticsEnabled = values[42] as Boolean,
            hapticsIntensity = values[43] as String,
            liquidGlassTuning = values[44] as LiquidGlassTuning,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        observeBoard()
        observeContinueWatching()
        observeDiscover()
        observeLibrary()
        observeAddons()
        refreshLibrary()
        observeCoreAuth()
        observeStreamingServer()
        restoreCoreSession()
        checkForUpdates(manual = true)

        startServer() // Always start the streaming server on app startup

        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                stopServerIfIdle()
            }
        }
    }

    private fun observeCoreAuth() {
        viewModelScope.launch {
            authRepository.getAccountStateFlow().collect { accountState ->
                account.value = accountState
                isRestoringSession.value = false
                if (accountState.isAuthenticated && accountState.authKey != null) {
                    authInFlight = false
                    authRepository.saveAuthKeyAndEmail(accountState.authKey, accountState.email ?: "")
                    refreshLibrary()
                    val email = accountState.email
                    if (!email.isNullOrBlank()) {
                        val hashedEmail = sha256(email)
                        com.posthog.PostHog.identify(hashedEmail)
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(hashedEmail)
                    }
                }
            }
        }
        viewModelScope.launch {
            authRepository.getProfileSettingsFlow().collect { settings ->
                profileSettings.value = settings
            }
        }
        viewModelScope.launch {
            authRepository.getTraktAuthFlow().collect { traktAuth ->
                if (traktAuth && !isTraktAuthenticated.value) {
                    com.posthog.PostHog.capture(event = "Trakt Authenticated")
                }
                isTraktAuthenticated.value = traktAuth
            }
        }
        viewModelScope.launch {
            core.events.collect { runtimeEvent ->
                val inner = runtimeEvent.event
                if (inner is RuntimeEvent.Event.CoreEvent && inner.value.type is Event.Type.Error && authInFlight) {
                    authInFlight = false
                    val message = (inner.value.type as Event.Type.Error).value.error
                    account.value = account.value.copy(isLoading = false, error = message)
                }
            }
        }
    }

    private fun observeStreamingServer() {
        viewModelScope.launch {
            core.streamingServerFlow().collect { server ->
                val content = server.settings.content
                if (content is com.stremio.core.models.LoadableSettings.Content.Ready) {
                    serverSettings.value = content.value
                }
            }
        }
        viewModelScope.launch {
            serverController.state.collect { state ->
                val stateName = when (state) {
                    is StreamingServerState.Ready -> "Ready"
                    is StreamingServerState.Failed -> "Failed"
                    StreamingServerState.Starting -> "Starting"
                    StreamingServerState.Stopped -> "Stopped"
                }
                com.posthog.PostHog.capture(
                    event = "Streaming Server State Changed",
                    properties = mapOf("new_state" to stateName)
                )
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey("server_state", stateName)
                if (state is StreamingServerState.Failed) {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey("server_error", formatServerErrorMessage(state.message))
                }

                if (state is StreamingServerState.Ready) {
                    fetchStreamingServerSettingsDirectly()
                }
            }
        }
    }

    private suspend fun startServerInternal() {
        lastServerActivityMs = System.currentTimeMillis()
        serverController.start()
        when (val state = serverController.state.value) {
            is StreamingServerState.Ready -> Unit
            is StreamingServerState.Failed -> error(state.message)
            StreamingServerState.Starting -> error("Streaming server is still starting")
            StreamingServerState.Stopped -> error("Streaming server did not start")
        }
    }

    private suspend fun stopServerIfIdle() {
        // No-op: Server should run continuously as long as the app is running
    }

    fun fetchStreamingServerSettingsDirectly() {
        viewModelScope.launch {
            val currentState = serverController.state.value
            if (currentState is StreamingServerState.Ready) {
                val url = "${currentState.baseUrl}/settings"
                val settingsObj = withContext(Dispatchers.IO) {
                    try {
                        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.setRequestProperty("Accept", "application/json")
                        if (connection.responseCode == 200) {
                            val body = connection.inputStream.bufferedReader().use { it.readText() }
                            JSONObject(body).optJSONObject("values")
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to fetch settings directly from server")
                        null
                    }
                }
                if (settingsObj != null) {
                    val current = serverSettings.value
                    val newSettings = com.stremio.core.models.StreamingServer.Settings(
                        appPath = settingsObj.optString("appPath", current?.appPath ?: ""),
                        cacheRoot = settingsObj.optString("cacheRoot", current?.cacheRoot ?: ""),
                        serverVersion = settingsObj.optString("serverVersion", current?.serverVersion ?: ""),
                        remoteHttps = if (settingsObj.has("remoteHttps") && !settingsObj.isNull("remoteHttps")) settingsObj.optString("remoteHttps") else null,
                        transcodeProfile = if (settingsObj.has("transcodeProfile") && !settingsObj.isNull("transcodeProfile")) settingsObj.optString("transcodeProfile") else null,
                        cacheSize = if (settingsObj.has("cacheSize") && !settingsObj.isNull("cacheSize")) settingsObj.optDouble("cacheSize") else null,
                        proxyStreamsEnabled = settingsObj.optBoolean("proxyStreamsEnabled", current?.proxyStreamsEnabled ?: true),
                        btMaxConnections = settingsObj.optLong("btMaxConnections", current?.btMaxConnections ?: 200L),
                        btHandshakeTimeout = settingsObj.optLong("btHandshakeTimeout", current?.btHandshakeTimeout ?: 200L),
                        btRequestTimeout = settingsObj.optLong("btRequestTimeout", current?.btRequestTimeout ?: 10L),
                        btDownloadSpeedSoftLimit = settingsObj.optDouble("btDownloadSpeedSoftLimit", current?.btDownloadSpeedSoftLimit ?: 1.0),
                        btDownloadSpeedHardLimit = settingsObj.optDouble("btDownloadSpeedHardLimit", current?.btDownloadSpeedHardLimit ?: 2.0),
                        btMinPeersForStable = settingsObj.optLong("btMinPeersForStable", current?.btMinPeersForStable ?: 10L)
                    )
                    serverSettings.value = newSettings
                    isSeedingEnabled.value = settingsObj.optBoolean("seedingEnabled", true)
                    core.updateStreamingServerSettings(newSettings)
                }
            }
        }
    }

    fun updateProfileSettings(newSettings: com.stremio.core.types.profile.Profile.Settings) {
        val oldSettings = profileSettings.value
        if (oldSettings != null) {
            val changes = mutableMapOf<String, Any>()
            if (oldSettings.playerType != newSettings.playerType) {
                changes["playerType"] = newSettings.playerType ?: ""
            }
            if (oldSettings.subtitlesAutoSelect != newSettings.subtitlesAutoSelect) {
                changes["subtitlesAutoSelect"] = newSettings.subtitlesAutoSelect
            }
            if (oldSettings.subtitlesLanguage != newSettings.subtitlesLanguage) {
                changes["subtitlesLanguage"] = newSettings.subtitlesLanguage ?: ""
            }
            if (oldSettings.subtitlesSize != newSettings.subtitlesSize) {
                changes["subtitlesSize"] = newSettings.subtitlesSize
            }
            if (oldSettings.subtitlesTextColor != newSettings.subtitlesTextColor) {
                changes["subtitlesTextColor"] = newSettings.subtitlesTextColor
            }
            if (oldSettings.subtitlesBackgroundColor != newSettings.subtitlesBackgroundColor) {
                changes["subtitlesBackgroundColor"] = newSettings.subtitlesBackgroundColor
            }
            if (oldSettings.subtitlesOutlineColor != newSettings.subtitlesOutlineColor) {
                changes["subtitlesOutlineColor"] = newSettings.subtitlesOutlineColor
            }
            if (oldSettings.assSubtitlesStyling != newSettings.assSubtitlesStyling) {
                changes["assSubtitlesStyling"] = newSettings.assSubtitlesStyling
            }
            if (oldSettings.audioLanguage != newSettings.audioLanguage) {
                changes["audioLanguage"] = newSettings.audioLanguage ?: ""
            }
            if (oldSettings.audioPassthrough != newSettings.audioPassthrough) {
                changes["audioPassthrough"] = newSettings.audioPassthrough
            }
            if (oldSettings.surroundSound != newSettings.surroundSound) {
                changes["surroundSound"] = newSettings.surroundSound
            }
            if (oldSettings.playInBackground != newSettings.playInBackground) {
                changes["playInBackground"] = newSettings.playInBackground
            }
            if (oldSettings.hardwareDecoding != newSettings.hardwareDecoding) {
                changes["hardwareDecoding"] = newSettings.hardwareDecoding
            }
            if (oldSettings.frameRateMatchingStrategy != newSettings.frameRateMatchingStrategy) {
                changes["frameRateMatchingStrategy"] = newSettings.frameRateMatchingStrategy.name ?: ""
            }
            if (oldSettings.seekTimeDuration != newSettings.seekTimeDuration) {
                changes["seekTimeDuration"] = newSettings.seekTimeDuration
            }
            if (oldSettings.bingeWatching != newSettings.bingeWatching) {
                changes["bingeWatching"] = newSettings.bingeWatching
            }
            if (oldSettings.nextVideoNotificationDuration != newSettings.nextVideoNotificationDuration) {
                changes["nextVideoNotificationDuration"] = newSettings.nextVideoNotificationDuration
            }

            for ((key, value) in changes) {
                com.posthog.PostHog.capture(
                    event = "Setting Changed",
                    properties = mapOf(
                        "setting_name" to key,
                        "new_value" to value
                    )
                )
            }
        }
        viewModelScope.launch {
            authRepository.updateSettings(newSettings)
        }
    }

    fun updateStreamingServerSettings(newSettings: com.stremio.core.models.StreamingServer.Settings) {
        viewModelScope.launch {
            serverSettings.value = newSettings
            core.updateStreamingServerSettings(newSettings)

            val currentState = serverController.state.value
            if (currentState is StreamingServerState.Ready) {
                val url = "${currentState.baseUrl}/settings"
                withContext(Dispatchers.IO) {
                    try {
                        val payloadObj = JSONObject().apply {
                            put("appPath", newSettings.appPath)
                            put("cacheRoot", newSettings.cacheRoot)
                            put("serverVersion", newSettings.serverVersion)
                            put("remoteHttps", newSettings.remoteHttps ?: JSONObject.NULL)
                            put("transcodeProfile", newSettings.transcodeProfile ?: JSONObject.NULL)
                            put("cacheSize", newSettings.cacheSize ?: JSONObject.NULL)
                            put("proxyStreamsEnabled", newSettings.proxyStreamsEnabled)
                            put("btMaxConnections", newSettings.btMaxConnections)
                            put("btHandshakeTimeout", newSettings.btHandshakeTimeout)
                            put("btRequestTimeout", newSettings.btRequestTimeout)
                            put("btDownloadSpeedSoftLimit", newSettings.btDownloadSpeedSoftLimit)
                            put("btDownloadSpeedHardLimit", newSettings.btDownloadSpeedHardLimit)
                            put("btMinPeersForStable", newSettings.btMinPeersForStable)
                            put("seedingEnabled", isSeedingEnabled.value)
                        }

                        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Accept", "application/json")

                        connection.outputStream.use { os ->
                            os.write(payloadObj.toString().toByteArray(Charsets.UTF_8))
                        }

                        val responseCode = connection.responseCode
                        if (responseCode == 200) {
                            Timber.d("Successfully updated settings directly on server")
                        } else {
                            Timber.e("Failed to update settings directly on server: response code %d", responseCode)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error posting settings to server")
                    }
                }
            }
        }
    }

    fun setSeedingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            isSeedingEnabled.value = enabled
            val currentSettings = serverSettings.value
            if (currentSettings != null) {
                updateStreamingServerSettings(currentSettings)
            }
        }
    }

    fun isAutoStartOnBoot(): Boolean = authRepository.isAutoStartOnBoot()
    fun setAutoStartOnBoot(enabled: Boolean) {
        authRepository.setAutoStartOnBoot(enabled)
        isAutoStartOnBoot.value = enabled
    }

    fun isServerInForeground(): Boolean = authRepository.isServerInForeground()
    fun setServerInForeground(enabled: Boolean) {
        val current = authRepository.isServerInForeground()
        if (current != enabled) {
            authRepository.setServerInForeground(enabled)
            isServerInForeground.value = enabled
            
            val packageManager = appContext.packageManager
            val intent = packageManager.getLaunchIntentForPackage(appContext.packageName)
            if (intent != null) {
                val componentName = intent.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                appContext.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }

    fun isMobileDataWarning(): Boolean = authRepository.isMobileDataWarning()
    fun setMobileDataWarning(enabled: Boolean) {
        authRepository.setMobileDataWarning(enabled)
        isMobileDataWarning.value = enabled
    }

    fun getSelectedFont(): AppFont = authRepository.getSelectedFont()
    fun setSelectedFont(font: AppFont) {
        authRepository.setSelectedFont(font)
        selectedFont.value = font
    }

    fun isKeepScreenOn(): Boolean = authRepository.isKeepScreenOn()
    fun setKeepScreenOn(enabled: Boolean) {
        authRepository.setKeepScreenOn(enabled)
        isKeepScreenOn.value = enabled
    }

    fun isAnalyticsEnabled(): Boolean = authRepository.isAnalyticsEnabled()
    fun setAnalyticsEnabled(enabled: Boolean) {
        authRepository.setAnalyticsEnabled(enabled)
        isAnalyticsEnabled.value = enabled
        
        if (enabled) {
            com.posthog.PostHog.optIn()
        } else {
            com.posthog.PostHog.optOut()
        }
        
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(enabled)
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        authRepository.setAutoUpdateEnabled(enabled)
        isAutoUpdateEnabled.value = enabled
        if (enabled) {
            maybeAutoCheckForUpdates()
        }
    }

    private fun maybeAutoCheckForUpdates() {
        if (!isAutoUpdateEnabled.value) return
        val lastCheckMs = authRepository.getLastUpdateCheckMs()
        if (System.currentTimeMillis() - lastCheckMs < UPDATE_CHECK_INTERVAL_MS) return
        checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean) {
        viewModelScope.launch {
            if (!manual) {
                if (!isAutoUpdateEnabled.value) return@launch
                val lastCheckMs = authRepository.getLastUpdateCheckMs()
                if (System.currentTimeMillis() - lastCheckMs < UPDATE_CHECK_INTERVAL_MS) return@launch
            }

            com.posthog.PostHog.capture(
                event = "Update Check Started",
                properties = mapOf("manual" to manual)
            )

            _updateState.value = UpdateState.Checking
            when (val result = runCatching { updateRepository.check() }.getOrElse { error ->
                UpdateState.Error(error.message ?: "Update check failed.")
            }) {
                is UpdateState.Available -> {
                    authRepository.setLastUpdateCheckMs(System.currentTimeMillis())
                    com.posthog.PostHog.capture(
                        event = "Update Check Finished",
                        properties = mapOf(
                            "status" to "Available",
                            "version" to result.info.tagName,
                            "manual" to manual
                        )
                    )
                    if (!manual && authRepository.getIgnoredUpdateVersion() == result.info.tagName) {
                        _updateState.value = UpdateState.Idle
                    } else {
                        _updateState.value = result
                    }
                }
                is UpdateState.UpToDate -> {
                    authRepository.setLastUpdateCheckMs(System.currentTimeMillis())
                    com.posthog.PostHog.capture(
                        event = "Update Check Finished",
                        properties = mapOf(
                            "status" to "UpToDate",
                            "manual" to manual
                        )
                    )
                    _updateState.value = result
                }
                is UpdateState.Error -> {
                    com.posthog.PostHog.capture(
                        event = "Update Check Finished",
                        properties = mapOf(
                            "status" to "Error",
                            "error_message" to result.message,
                            "manual" to manual
                        )
                    )
                    if (manual) {
                        _updateState.value = result
                    } else {
                        Timber.e("Auto update check failed: ${result.message}")
                        _updateState.value = UpdateState.Idle
                    }
                }
                else -> {
                    _updateState.value = result
                }
            }
        }
    }

    fun downloadAndInstallUpdate(info: UpdateInfo) {
        viewModelScope.launch {
            runCatching {
                val file = updateRepository.download(info) { bytesRead, totalBytes ->
                    _updateState.value = UpdateState.Downloading(
                        bytesRead = bytesRead,
                        totalBytes = totalBytes,
                        progress = totalBytes?.takeIf { it > 0L }?.let { bytesRead.toFloat() / it.toFloat() },
                    )
                }
                installDownloadedUpdate(file)
            }.onFailure { error ->
                Timber.e(error, "Failed to download update")
                _updateState.value = UpdateState.Error(error.message ?: "Update download failed.")
            }
        }
    }

    fun installDownloadedUpdate(file: File) {
        runCatching {
            val needsPermission = apkInstaller.install(file)
            _updateState.value = UpdateState.ReadyToInstall(
                file = file,
                needsUnknownSourcesPermission = needsPermission,
            )
        }.onFailure { error ->
            Timber.e(error, "Failed to launch APK installer")
            _updateState.value = UpdateState.Error(error.message ?: "Could not open the system installer.")
        }
    }

    fun ignoreUpdate(tagName: String) {
        authRepository.setIgnoredUpdateVersion(tagName)
        _updateState.value = UpdateState.Idle
    }

    fun dismissUpdateDialog() {
        _updateState.value = UpdateState.Idle
    }

    fun acknowledgeAnalyticsDisclosure(enableAnalytics: Boolean) {
        authRepository.setAnalyticsDisclosureAcknowledged(true)
        showAnalyticsDisclosure.value = false
        setAnalyticsEnabled(enableAnalytics)
    }

    fun showAnalyticsDisclosure() {
        showAnalyticsDisclosure.value = true
    }

    fun setMinSeedsThreshold(value: Int) {
        authRepository.setMinSeedsThreshold(value)
        minSeedsThreshold.value = value
    }

    fun setMinDownloadSpeedBps(value: Long) {
        authRepository.setMinDownloadSpeedBps(value)
        minDownloadSpeedBps.value = value
    }

    fun setPreferredQuality(value: String) {
        authRepository.setPreferredQuality(value)
        preferredQuality.value = value
    }

    fun setGlobalUiStyle(value: String) {
        authRepository.setGlobalUiStyle(value)
        globalUiStyle.value = if (value == "modern") "modern" else "classic"
    }

    fun setPlayerUiStyle(value: String) {
        authRepository.setPlayerUiStyle(value)
        playerUiStyle.value = when (value) {
            "classic" -> "classic"
            "modern" -> "modern"
            else -> "global"
        }
    }

    fun setGlassEffectsMode(value: String) {
        val resolved = when (value) {
            "full" -> "full"
            "static" -> "static"
            else -> "balanced"
        }
        authRepository.setGlassEffectsMode(resolved)
        glassEffectsMode.value = resolved
    }

    fun setGlobalGlassAlpha(value: Float) {
        authRepository.setGlobalGlassAlpha(value)
        globalGlassAlpha.value = value
    }

    fun setAdaptiveGlassContrastEnabled(enabled: Boolean) {
        authRepository.setAdaptiveGlassContrastEnabled(enabled)
        adaptiveGlassContrast.value = enabled
    }

    fun setGlassHapticsEnabled(enabled: Boolean) {
        authRepository.setGlassHapticsEnabled(enabled)
        glassHapticsEnabled.value = enabled
    }

    fun setHapticsIntensity(value: String) {
        authRepository.setHapticsIntensity(value)
        hapticsIntensity.value = value
    }

    fun setLiquidGlassTuning(value: LiquidGlassTuning) {
        val tuning = value.clamped()
        authRepository.setLiquidGlassTuning(tuning)
        liquidGlassTuning.value = tuning
    }

    fun resetLiquidGlassTuning() {
        setLiquidGlassTuning(LiquidGlassTuning())
    }

    fun setAutoSwitchOnDeadStream(enabled: Boolean) {
        authRepository.setAutoSwitchOnDeadStream(enabled)
        isAutoSwitchOnDeadStream.value = enabled
    }

    fun isTraktAuthenticated(): Boolean = authRepository.isTraktAuthenticated()

    fun authenticateTrakt(context: Context) {
        authRepository.authenticateTrakt(context)
    }

    fun logoutTrakt() {
        viewModelScope.launch {
            authRepository.logoutTrakt()
        }
        com.posthog.PostHog.capture(event = "Trakt Logged Out")
    }

    fun installTraktAddon() {
        viewModelScope.launch {
            authRepository.installTraktAddon()
        }
        com.posthog.PostHog.capture(event = "Trakt Addon Installed")
    }

    private fun restoreCoreSession() {
        if (authRepository.isAuthenticated()) {
            isRestoringSession.value = false
            return
        }
        val authKey = authRepository.getSavedAuthKey()
        if (!authKey.isNullOrBlank() && authKey != "mock_auth_key") {
            isRestoringSession.value = true
            viewModelScope.launch {
                val result = withTimeoutOrNull(5000) {
                    runCatching { authRepository.loginWithToken(authKey) }
                }
                if (result == null || result.isFailure) {
                    isRestoringSession.value = false
                }
            }
        } else {
            isRestoringSession.value = false
        }
    }

    private fun observeBoard() {
        viewModelScope.launch {
            boardRepository.getBoardFlow()
                .map { board -> board to boardRepository.extractBoardShelves(board) }
                .flowOn(Dispatchers.Default)
                .collect { (board, shelves) ->
                    cachedBoard = board
                    boardShelves.value = shelves
                    if (board.catalogs.isNotEmpty()) {
                        val toPreload = board.catalogs.take(5)
                            .flatMap { catalog -> catalog.pages.filter { it.content == null } }
                            .map { it.request.toString() }
                            .filter { it !in requestedRequests }
                        if (toPreload.isNotEmpty()) {
                            requestedRequests.addAll(toPreload)
                            boardRepository.loadBoardRange(0, minOf(board.catalogs.size - 1, 4))
                        }
                    }
                }
        }
    }

    fun onShelfVisible(shelfIndex: Int) {
        val board = cachedBoard ?: return
        var currentShelfIdx = 0
        var foundCatalogIdx = -1
        for (cIdx in board.catalogs.indices) {
            val catalog = board.catalogs[cIdx]
            val pageCount = catalog.pages.size
            if (shelfIndex >= currentShelfIdx && shelfIndex < currentShelfIdx + pageCount) {
                foundCatalogIdx = cIdx
                break
            }
            currentShelfIdx += pageCount
        }
        if (foundCatalogIdx != -1) {
            val start = foundCatalogIdx
            val end = minOf(foundCatalogIdx + 2, board.catalogs.size - 1)
            var needLoad = false
            for (i in start..end) {
                if (i in board.catalogs.indices) {
                    val catalog = board.catalogs[i]
                    for (page in catalog.pages) {
                        if (page.content == null) {
                            val reqStr = page.request.toString()
                            if (!requestedRequests.contains(reqStr)) {
                                requestedRequests.add(reqStr)
                                needLoad = true
                            }
                        }
                    }
                }
            }
            if (needLoad) {
                Timber.d("onShelfVisible(%d): catalogIndex=%d. Loading range %d..%d", shelfIndex, foundCatalogIdx, start, end)
                boardRepository.loadBoardRange(start, end)
            }
        }
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            boardRepository.getContinueWatchingFlow().collect { cwShelf ->
                continueWatching.value = cwShelf
            }
        }
    }

    private fun observeDiscover() {
        viewModelScope.launch {
            catalogRepository.getDiscoverFlow().collect { discover ->
                discoverCatalogWithFilters.value = discover
                var req = discoverCatalogRequest.value
                if (req == null) {
                    val movieType = discover.selectable.types.find { it.type.lowercase() == "movie" }
                    if (movieType != null) {
                        req = movieType.request
                    } else {
                        req = discover.selected?.request
                    }
                    if (req == null && discover.selectable.types.isNotEmpty()) {
                        req = discover.selectable.types.first().request
                    }
                    if (req != null) {
                        discoverCatalogRequest.value = req
                        discoverCatalogTitle.value = "Discover"
                        catalogRepository.loadDiscover(req)
                    }
                }

                val currentReq = discoverCatalogRequest.value
                if (currentReq != null && (discover.selected?.request == currentReq)) {
                    val items = catalogRepository.extractDiscoverItems(discover)
                    val isLoading = discover.catalog.pages.any { it.content is com.stremio.core.models.LoadablePage.Content.Loading || it.content == null }
                    discoverCatalog.value = CatalogShelf(
                        title = discoverCatalogTitle.value ?: "Discover",
                        items = items,
                        isLoading = isLoading,
                        seeAllRequest = currentReq
                    )
                }
            }
        }
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            catalogRepository.getLibraryFlow().collect { libraryWithFiltersVal ->
                libraryWithFilters.value = libraryWithFiltersVal
            }
        }
        viewModelScope.launch {
            catalogRepository.getLibraryShelfFlow().collect { shelf ->
                library.value = shelf
            }
        }
    }

    fun openDiscoverCatalog(request: com.stremio.core.types.addon.ResourceRequest, title: String) {
        isDiscoverSeeAll.value = true
        discoverCatalogRequest.value = request
        discoverCatalogTitle.value = title
        discoverCatalog.value = CatalogShelf(title = title, isLoading = true, seeAllRequest = request)
        selectedSection.value = MainSection.Discover
        catalogRepository.loadDiscover(request)
    }

    fun closeDiscoverCatalog() {
        isDiscoverSeeAll.value = false
        discoverCatalogRequest.value = null
        discoverCatalogTitle.value = null
        discoverCatalog.value = CatalogShelf(title = "Discover", isLoading = false)
        selectedSection.value = MainSection.Home
    }

    fun openStreams(item: CatalogItem) {
        streamsJob?.cancel()
        val rememberedSelection = localContinueWatchingStreamSelection(item)
        if (rememberedSelection != null) {
            openRememberedContinueWatchingStream(item, rememberedSelection)
            return
        }

        streamsJob = launchStreamsMenuJob(item)
    }

    private fun openRememberedContinueWatchingStream(item: CatalogItem, rememberedSelection: LocalStreamSelection) {
        val isSeries = item.type == "series"
        streams.value = StreamsUiState(
            forItem = item,
            isOpen = false,
            isLoading = true,
            isSeries = isSeries,
            selectedVideoId = if (isSeries) item.continueWatchingVideoId else null,
        )

        streamsJob = viewModelScope.launch {
            runCatching { startServerInternal() }

            val videoId = if (isSeries) item.continueWatchingVideoId else null
            val matchedOption = runCatching {
                withTimeoutOrNull(30_000) {
                    catalogRepository.getMetaDetailsFlow(
                        type = item.type,
                        id = item.id,
                        videoId = videoId,
                        guessStreamPath = !isSeries,
                    )
                        .map { details ->
                            if (isSeries && videoId != null) {
                                episodeDisplayInfo(details, videoId)?.let { episode ->
                                    streams.value = streams.value.copy(
                                        selectedEpisodeLabel = episode.label,
                                        releaseDateLabel = episode.releaseDate,
                                    )
                                }
                            }
                            catalogRepository.extractStreams(details)
                                .mapIndexed { index, coreStream -> buildStreamOption(index, coreStream) }
                                .firstOrNull { rememberedSelection.matches(it) }
                        }
                        .first { it != null }
                }
            }.getOrNull()

            if (matchedOption != null) {
                playStream(matchedOption)
            } else {
                streams.value = StreamsUiState()
                streamsJob = launchStreamsMenuJob(item)
            }
        }
    }

    private fun localContinueWatchingStreamSelection(item: CatalogItem): LocalStreamSelection? {
        if (!item.isContinueWatching) return null
        val videoId = continueWatchingVideoKey(item)
        return authRepository.getLocalStreamSelection(item.type, item.id, videoId)
    }

    private fun continueWatchingVideoKey(item: CatalogItem): String {
        return item.continueWatchingVideoId?.takeIf { it.isNotBlank() } ?: item.id
    }

    private fun launchStreamsMenuJob(item: CatalogItem): Job {
        val isSeries = item.type == "series"
        streams.value = StreamsUiState(forItem = item, isOpen = true, isLoading = true, isSeries = isSeries)

        return viewModelScope.launch {
            runCatching { startServerInternal() }

            if (isSeries) {
                val collector = launch {
                    catalogRepository.getMetaDetailsFlow(type = item.type, id = item.id, videoId = null, guessStreamPath = false)
                        .collect { details ->
                            val videos = catalogRepository.extractVideos(details)
                            if (videos.isNotEmpty()) {
                                val seasons = videos.mapNotNull { it.seriesInfo?.season?.toInt() }.distinct().sorted()
                                val defaultSeason = defaultSeasonForVideos(videos) ?: seasons.firstOrNull()
                                val episodes = videos.map { video ->
                                    EpisodeOption(
                                        videoId = video.id,
                                        season = video.seriesInfo?.season?.toInt() ?: 0,
                                        episode = video.seriesInfo?.episode?.toInt() ?: 0,
                                        title = video.title,
                                        thumbnail = video.thumbnail,
                                        releaseDate = formatReleaseDate(video.released),
                                        watched = video.watched,
                                        isCurrent = video.currentVideo,
                                    )
                                }
                                if (streams.value.isOpen) {
                                    streams.value = streams.value.copy(
                                        isLoading = false,
                                        episodes = episodes,
                                        seasons = seasons,
                                        selectedSeason = defaultSeason,
                                    )
                                }
                            }
                        }
                }
                withTimeoutOrNull(15_000) { collector.join() }
                if (streams.value.isOpen && streams.value.isLoading) {
                    streams.value = streams.value.copy(
                        isLoading = false,
                        error = if (streams.value.episodes.isEmpty()) "No episodes found." else null,
                    )
                }
            } else {
                val collector = launch {
                    catalogRepository.getMetaDetailsFlow(type = item.type, id = item.id, videoId = null, guessStreamPath = true)
                        .collect { details ->
                            val options = catalogRepository.extractStreams(details).mapIndexed { index, coreStream ->
                                buildStreamOption(index, coreStream)
                            }
                            if (streams.value.isOpen) {
                                streams.value = streams.value.copy(
                                    streams = options,
                                    releaseDateLabel = movieReleaseDate(details),
                                    isLoading = options.isEmpty(),
                                )
                            }
                        }
                }
                withTimeoutOrNull(30_000) { collector.join() }
                if (streams.value.isOpen && streams.value.isLoading) {
                    streams.value = streams.value.copy(
                        isLoading = false,
                        error = if (streams.value.streams.isEmpty()) "No streams found. Install a stream addon to watch." else null,
                    )
                }
            }
        }
    }

    fun selectEpisode(episode: EpisodeOption) {
        val item = streams.value.forItem ?: return
        streamsJob?.cancel()
        streams.value = streams.value.copy(
            selectedVideoId = episode.videoId,
            selectedEpisodeLabel = episodeDisplayLabel(episode.season, episode.episode, episode.title),
            releaseDateLabel = episode.releaseDate,
            isLoading = true,
            streams = emptyList(),
            error = null,
        )
        streamsJob = viewModelScope.launch {
            runCatching { startServerInternal() }
            val collector = launch {
                catalogRepository.getMetaDetailsFlow(type = item.type, id = item.id, videoId = episode.videoId, guessStreamPath = false)
                    .collect { details ->
                        val options = catalogRepository.extractStreams(details).mapIndexed { index, coreStream ->
                            buildStreamOption(index, coreStream)
                        }
                        if (streams.value.isOpen) {
                            streams.value = streams.value.copy(
                                streams = options,
                                isLoading = options.isEmpty(),
                            )
                        }
                    }
            }
            withTimeoutOrNull(30_000) { collector.join() }
            if (streams.value.isOpen && streams.value.isLoading) {
                streams.value = streams.value.copy(
                    isLoading = false,
                    error = if (streams.value.streams.isEmpty()) "No streams found. Install a stream addon to watch." else null,
                )
            }
        }
    }

    fun selectSeason(season: Int) {
        streams.value = streams.value.copy(selectedSeason = season)
    }

    fun selectStreamProvider(provider: String?) {
        streams.value = streams.value.copy(selectedProvider = provider)
    }

    fun selectStreamSortCriterion(criterion: StreamSortCriterion) {
        streams.value = streams.value.copy(sortCriterion = criterion)
    }

    fun backToEpisodes() {
        streams.value = streams.value.copy(
            selectedVideoId = null,
            selectedEpisodeLabel = null,
            releaseDateLabel = null,
            streams = emptyList(),
            error = null,
        )
    }

    private fun defaultSeasonForVideos(videos: List<com.stremio.core.types.resource.Video>): Int? {
        val currentVideo = videos.firstOrNull { it.currentVideo }
        val lastProgressedVideo = videos
            .filter { it.watched || (it.progress ?: 0.0) > 0.0 }
            .maxWithOrNull(
                compareBy<com.stremio.core.types.resource.Video> { it.seriesInfo?.season ?: 0L }
                    .thenBy { it.seriesInfo?.episode ?: 0L }
            )
        return (currentVideo ?: lastProgressedVideo)?.seriesInfo?.season?.toInt()
    }

    private fun movieReleaseDate(details: com.stremio.core.models.MetaDetails): String? {
        val content = details.metaItem?.content
        return if (content is com.stremio.core.models.LoadableMetaItem.Content.Ready) {
            formatReleaseDate(content.value.released) ?: content.value.releaseInfo
        } else {
            streams.value.forItem?.releaseInfo
        }
    }

    private data class EpisodeDisplayInfo(
        val label: String,
        val releaseDate: String?,
    )

    private fun episodeDisplayInfo(
        details: com.stremio.core.models.MetaDetails,
        videoId: String,
    ): EpisodeDisplayInfo? {
        return catalogRepository.extractVideos(details)
            .firstOrNull { it.id == videoId }
            ?.let { video ->
                EpisodeDisplayInfo(
                    label = episodeDisplayLabel(
                        season = video.seriesInfo?.season?.toInt(),
                        episode = video.seriesInfo?.episode?.toInt(),
                        title = video.title,
                    ) ?: video.title,
                    releaseDate = formatReleaseDate(video.released),
                )
            }
    }

    private fun episodeDisplayLabel(season: Int?, episode: Int?, title: String?): String? {
        val cleanTitle = title?.takeIf { it.isNotBlank() }
        val number = if ((season ?: 0) > 0 && (episode ?: 0) > 0) {
            "S${season}E${episode}"
        } else {
            null
        }
        return listOfNotNull(cleanTitle, number)
            .joinToString(" · ")
            .takeIf { it.isNotBlank() }
    }

    private fun playbackDisplayTitle(): String? {
        val current = streams.value
        val item = current.forItem ?: return null
        return if (current.isSeries) {
            current.selectedEpisodeLabel?.takeIf { it.isNotBlank() } ?: item.name
        } else {
            item.name
        }
    }

    private fun formatReleaseDate(timestamp: pbandk.wkt.Timestamp?): String? {
        val seconds = timestamp?.seconds ?: return null
        if (seconds <= 0L) return null
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(seconds * 1000L))
    }

    private fun buildStreamOption(index: Int, coreStream: CoreStream): StreamOption {
        val rawDescription = coreStream.stream.description?.takeIf { it.isNotBlank() }
            ?: coreStream.stream.thumbnail
        val parsed = parseStreamDescription(rawDescription)
        val quality = coreStream.stream.name?.let { name ->
            val resolutions = listOf("2160p", "4k", "1080p", "720p", "480p")
            resolutions.firstOrNull { name.contains(it, ignoreCase = true) }
        }
        return StreamOption(
            key = "$index-${coreStream.addonTitle}-${coreStream.stream.name ?: ""}-${rawDescription ?: ""}",
            name = coreStream.stream.name?.takeIf { it.isNotBlank() } ?: coreStream.addonTitle,
            description = rawDescription,
            addonTitle = coreStream.addonTitle,
            quality = quality,
            core = coreStream,
            seeds = parsed.seeds,
            size = parsed.size,
            origin = parsed.origin,
            cleanDescription = parsed.cleanDescription,
        )
    }

    fun closeStreams() {
        streamsJob?.cancel()
        streamsJob = null
        streams.value = StreamsUiState()
    }

    private fun restoreStreamsAfterPlayer() {
        val current = streams.value
        val item = current.forItem ?: return
        if (current.isSeries) {
            if (current.episodes.isNotEmpty()) {
                streams.value = current.copy(
                    isOpen = true,
                    isResolving = false,
                    isLoading = false,
                    selectedVideoId = null,
                    selectedEpisodeLabel = null,
                    releaseDateLabel = null,
                    streams = emptyList(),
                    error = null,
                )
            } else {
                streamsJob?.cancel()
                streamsJob = launchStreamsMenuJob(item)
            }
        } else if (current.streams.isNotEmpty()) {
            streams.value = current.copy(isOpen = true, isResolving = false, isLoading = false)
        } else {
            streamsJob?.cancel()
            streamsJob = launchStreamsMenuJob(item)
        }
    }

    fun playStream(option: StreamOption) {
        if (isMobileDataWarning.value && isUsingMobileData()) {
            pendingMobileDataStream.value = option
        } else {
            proceedWithPlayback(option)
        }
    }

    fun confirmMobileDataPlayback() {
        val option = pendingMobileDataStream.value ?: return
        pendingMobileDataStream.value = null
        proceedWithPlayback(option)
    }

    fun cancelMobileDataPlayback() {
        pendingMobileDataStream.value = null
    }

    private fun isUsingMobileData(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun proceedWithPlayback(option: StreamOption) {
        playJob?.cancel()
        subtitleObserverJob?.cancel()
        streams.value = streams.value.copy(isResolving = true, error = null)
        playJob = viewModelScope.launch {
            val engine = PlayerEngine.fromProfileValue(profileSettings.value?.playerType)
            if (streamRequiresLocalServer(option)) {
                runCatching { startServerInternal() }
                    .onFailure { error ->
                        val errorMsg = "Streaming server failed to start: ${error.message ?: "Unknown error"}"
                        com.posthog.PostHog.capture(
                            event = "Stream Playback Failed",
                            properties = mapOf(
                                "player_engine" to engine.name,
                                "error_message" to errorMsg
                            )
                        )
                        streams.value = streams.value.copy(
                            isResolving = false,
                            error = errorMsg,
                        )
                        return@launch
                    }
            }
            val loaded = playbackRepository.resolveAndLoadStream(
                option = option,
                engine = engine,
                displayTitle = playbackDisplayTitle(),
            )
            if (!loaded) {
                com.posthog.PostHog.capture(
                    event = "Stream Playback Failed",
                    properties = mapOf(
                        "player_engine" to engine.name,
                        "error_message" to "Could not resolve a playable stream."
                    )
                )
                streams.value = streams.value.copy(isResolving = false, error = "Could not resolve a playable stream.")
                return@launch
            }
            com.posthog.PostHog.capture(
                event = "Stream Playback Started",
                properties = mapOf(
                    "stream_type" to option.addonTitle,
                    "player_engine" to engine.name,
                    "is_resolving" to streamRequiresLocalServer(option)
                )
            )
            rememberLocalStreamSelection(option)
            lastPlayedOption = option
            nextVideo.value = null
            showNextVideoPopup.value = false
            dismissedNextVideoId = null
            lastTimeReportMs = 0L
            showNoSeedsBanner.value = false
            noSeedsReason.value = null
            unhealthySinceMs = null
            streams.value = streams.value.copy(isResolving = false, isOpen = false)
            playerOpen.value = true
            observeAddonSubtitlesForPlayback()
            startHealthWatch(option)
        }
    }

    private fun streamRequiresLocalServer(option: StreamOption): Boolean {
        if (option.core.stream.source is com.stremio.core.types.resource.Stream.Source.Tramvai) {
            return true
        }
        val directUrl = runCatching { core.directUrl(option.core.stream) }.getOrNull()
        return directUrl?.startsWith(StremioCore.STREAMING_SERVER_BASE) == true
    }

    private fun rememberLocalStreamSelection(option: StreamOption) {
        val currentStreams = streams.value
        val item = currentStreams.forItem ?: return
        val videoId = currentStreams.selectedVideoId
            ?: item.continueWatchingVideoId
            ?: item.id
        authRepository.rememberLocalStreamSelection(item.type, item.id, videoId, option)
    }

    fun closePlayer() {
        playJob?.cancel()
        playJob = null
        nextVideoJob?.cancel()
        nextVideoJob = null
        subtitleObserverJob?.cancel()
        subtitleObserverJob = null
        noSeedsPollJob?.cancel()
        noSeedsPollJob = null
        playbackRepository.release()
        playerOpen.value = false
        nextVideo.value = null
        showNextVideoPopup.value = false
        showNoSeedsBanner.value = false
        noSeedsReason.value = null
        restoreStreamsAfterPlayer()

        lastServerActivityMs = 0L
        viewModelScope.launch {
            stopServerIfIdle()
        }
    }

    /** Polls live torrent health for a torrent stream and surfaces a fallback when it's dead/too slow. */
    private fun startHealthWatch(option: StreamOption) {
        noSeedsPollJob?.cancel()
        unhealthySinceMs = null
        val source = option.core.stream.source
        if (source !is com.stremio.core.types.resource.Stream.Source.Tramvai) return
        val infoHash = source.value.infoHash
        val fileIndex = source.value.fileIdx ?: StremioCore.STREAMING_SERVER_AUTO_FILE_INDEX

        noSeedsPollJob = viewModelScope.launch {
            while (true) {
                playbackRepository.requestStreamStatistics(infoHash, fileIndex)
                // Local JNI/HTTP roundtrip to the on-device streaming server; comfortably finishes well under this.
                delay(800)
                val stats = playbackRepository.getStreamStatistics()
                if (stats != null && stats.infoHash == infoHash) {
                    val tooFewSeeds = stats.peers < minSeedsThreshold.value
                    val tooSlow = minDownloadSpeedBps.value > 0 && stats.downloadSpeed < minDownloadSpeedBps.value
                    val isNearComplete = stats.streamProgress >= 0.95
                    if ((tooFewSeeds || tooSlow) && !isNearComplete) {
                        val since = unhealthySinceMs ?: System.currentTimeMillis().also { unhealthySinceMs = it }
                        if (System.currentTimeMillis() - since >= 20_000) {
                            val reason = if (tooFewSeeds) "No seeds found for this stream." else "This stream is downloading too slowly."
                            if (isAutoSwitchOnDeadStream.value && playNextBestStream()) {
                                return@launch
                            }
                            noSeedsReason.value = reason
                            showNoSeedsBanner.value = true
                        }
                    } else {
                        unhealthySinceMs = null
                        showNoSeedsBanner.value = false
                    }
                }
                delay(2_200)
            }
        }
    }

    /** Switches to the best remaining candidate stream, ranked by seeds then closeness to the preferred quality. Returns false if there was nothing else to try. */
    fun playNextBestStream(): Boolean {
        val current = lastPlayedOption
        val candidates = streams.value.streams.filter { it.key != current?.key }
        val best = candidates.sortedWith(
            compareByDescending<StreamOption> { parseSeedCount(it.seeds) }
                .thenByDescending { qualityScore(it.quality, preferredQuality.value) }
        ).firstOrNull() ?: return false
        showNoSeedsBanner.value = false
        playStream(best)
        return true
    }

    /** Called every ~500ms by [PlayerScreen]'s position tracker. */
    fun onPlayerTick(positionMs: Long, durationMs: Long) {
        lastServerActivityMs = System.currentTimeMillis()
        if (durationMs <= 0) return

        if (positionMs - lastTimeReportMs >= 5_000 || lastTimeReportMs == 0L) {
            lastTimeReportMs = positionMs
            playbackRepository.reportTimeChanged(positionMs, durationMs)
        }

        val next = nextVideo.value ?: playbackRepository.getNextVideo()?.also { nextVideo.value = it }
        if (next == null) {
            showNextVideoPopup.value = false
            return
        }

        val notificationDurationMs = profileSettings.value?.nextVideoNotificationDuration ?: return
        val remainingMs = durationMs - positionMs
        showNextVideoPopup.value = remainingMs in 0..notificationDurationMs && dismissedNextVideoId != next.id
    }

    fun onPlayerSeek(positionMs: Long, durationMs: Long) {
        lastTimeReportMs = positionMs
        playbackRepository.reportSeek(positionMs, durationMs)
    }

    fun onPlayerPausedChanged(paused: Boolean) {
        playbackRepository.reportPausedChanged(paused)
    }

    fun dismissNextVideoPopup() {
        dismissedNextVideoId = nextVideo.value?.id
        showNextVideoPopup.value = false
    }

    /** Called when ExoPlayer reaches the end of the current stream. */
    fun onPlaybackEnded() {
        playbackRepository.reportEnded()
        val next = nextVideo.value
        if (profileSettings.value?.bingeWatching == true && next != null) {
            playNextVideo(next)
        } else {
            closePlayer()
        }
    }

    /** Plays [video] next: re-resolves its streams and auto-picks one, skipping the streams sheet. */
    fun playNextVideo(video: com.stremio.core.types.resource.Video) {
        val item = streams.value.forItem ?: return
        playbackRepository.reportNextVideo()
        showNextVideoPopup.value = false
        streams.value = streams.value.copy(
            selectedVideoId = video.id,
            selectedEpisodeLabel = episodeDisplayLabel(
                season = video.seriesInfo?.season?.toInt(),
                episode = video.seriesInfo?.episode?.toInt(),
                title = video.title,
            ),
            releaseDateLabel = formatReleaseDate(video.released),
        )
        nextVideoJob?.cancel()
        nextVideoJob = viewModelScope.launch {
            runCatching { startServerInternal() }
            val collector = launch {
                catalogRepository.getMetaDetailsFlow(type = item.type, id = item.id, videoId = video.id, guessStreamPath = false)
                    .collect { details ->
                        val options = catalogRepository.extractStreams(details).mapIndexed { index, coreStream ->
                            buildStreamOption(index, coreStream)
                        }
                        if (options.isNotEmpty()) {
                            val preferred = lastPlayedOption?.let { last ->
                                options.firstOrNull { it.addonTitle == last.addonTitle }
                            }
                            playStream(preferred ?: options.first())
                            nextVideoJob?.cancel()
                        }
                    }
            }
            withTimeoutOrNull(30_000) { collector.join() }
        }
    }

    fun attachPlayerView(view: android.view.View) = playbackRepository.attachView(view)

    fun detachPlayerView() = playbackRepository.detachView()

    fun getSubtitlePrefs(): Pair<Int, Int> = playbackRepository.getSubtitlePrefs()

    fun updateSubtitlePrefs(sizePercent: Int, offsetPercent: Int) {
        playbackRepository.updateSubtitlePrefs(sizePercent, offsetPercent)
    }

    fun getPlayerStreamState(): com.stremio.core.models.Player.StreamState? =
        playbackRepository.getPlayerStreamState()

    fun rememberAudioTrack(track: PlayerTrackOption) {
        playbackRepository.rememberAudioTrack(track)
    }

    fun rememberSubtitleTrack(track: PlayerTrackOption) {
        playbackRepository.rememberSubtitleTrack(track)
    }

    fun rememberSubtitlesDisabled() {
        playbackRepository.rememberSubtitlesDisabled()
    }

    fun rememberSubtitleStyle(style: PlayerSubtitleStyle) {
        playbackRepository.rememberSubtitleStyle(style)
    }

    fun importLocalSubtitle(uri: Uri) {
        viewModelScope.launch {
            val track = withContext(Dispatchers.IO) { copyLocalSubtitle(uri) } ?: return@launch
            playbackRepository.addLocalSubtitle(track)
        }
    }

    fun getPlayer(): com.stremio.mobile.player.Player? = playbackRepository.getPlayer()

    fun acceptIntent(intent: Intent?) {
        latestIntentUri.value = intent?.dataString
    }

    fun startServer() {
        viewModelScope.launch {
            runCatching { startServerInternal() }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            serverController.stop()
        }
    }

    fun onAppForegrounded() {
        // No-op for now, symmetry/future use
    }

    fun onAppBackgrounded() {
        viewModelScope.launch {
            stopServerIfIdle()
        }
    }

    fun checkServerWorking() {
        viewModelScope.launch {
            isPingLoading.value = true
            serverPingStatus.value = "Checking..."
            val currentState = serverController.state.value
            if (currentState is StreamingServerState.Ready) {
                val url = "${currentState.baseUrl}/heartbeat"
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        val responseCode = connection.responseCode
                        if (responseCode == 200) {
                            val text = connection.inputStream.bufferedReader().use { it.readText() }
                            text.contains("success")
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                if (ok) {
                    serverPingStatus.value = "Online"
                } else {
                    serverPingStatus.value = "Offline (no response)"
                }
            } else {
                serverPingStatus.value = "Server is not started"
            }
            isPingLoading.value = false
        }
    }

    fun refreshCatalogs() {
        requestedRequests.clear()
        boardRepository.loadBoard()
    }

    fun selectSection(section: MainSection) {
        selectedSection.value = section
        isDiscoverSeeAll.value = false
        if (section == MainSection.Settings) {
            fetchStreamingServerSettingsDirectly()
        }
        if (section == MainSection.Library) {
            refreshLibrary()
        }
        if (section == MainSection.Discover) {
            val discover = catalogRepository.getDiscover()
            var req = discoverCatalogRequest.value
            if (req == null) {
                val movieType = discover.selectable.types.find { it.type.lowercase() == "movie" }
                if (movieType != null) {
                    req = movieType.request
                } else {
                    req = discover.selected?.request
                }
                if (req == null && discover.selectable.types.isNotEmpty()) {
                    req = discover.selectable.types.first().request
                }
            }
            if (req != null) {
                discoverCatalogRequest.value = req
                discoverCatalogTitle.value = "Discover"
                catalogRepository.loadDiscover(req)
            }
        }
    }

    fun search(query: String) {
        searchQuery.value = query
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            searchResults.value = CatalogShelf(title = "Search", isLoading = false)
            return
        }
        searchResults.value = CatalogShelf(title = "Search", isLoading = true)
        searchShelves.value = emptyList()
        searchJob = viewModelScope.launch {
            delay(300)
            val collector = launch {
                catalogRepository.search(trimmed)
                    .map { board -> board to boardRepository.extractBoardShelves(board).filter { it.items.isNotEmpty() } }
                    .flowOn(Dispatchers.Default)
                    .collect { (board, shelves) ->
                        val toPreload = board.catalogs
                            .flatMap { catalog -> catalog.pages.filter { it.content == null } }
                            .map { it.request.toString() }
                            .filter { it !in requestedRequests }
                        if (board.catalogs.isNotEmpty() && toPreload.isNotEmpty()) {
                            requestedRequests.addAll(toPreload)
                            catalogRepository.loadSearchRange(0, minOf(board.catalogs.size - 1, 14))
                        }
                        searchShelves.value = shelves
                        val items = shelves.flatMap { it.items }.distinctBy { "${it.type}-${it.id}" }
                        searchResults.value = CatalogShelf(
                            title = "Search",
                            items = items,
                            isLoading = false
                        )
                    }
            }
            withTimeoutOrNull(15_000) { collector.join() }
            if (searchResults.value.isLoading) {
                searchResults.value = searchResults.value.copy(
                    isLoading = false,
                    error = if (searchShelves.value.isEmpty()) "No results for \"$trimmed\"." else null,
                )
            }
            com.posthog.PostHog.capture(
                event = "Content Searched",
                properties = mapOf(
                    "query_length" to trimmed.length,
                    "has_results" to searchResults.value.items.isNotEmpty()
                )
            )
        }
    }

    fun openSearch() {
        isSearchOpen.value = true
    }

    fun clearSearch() {
        searchJob?.cancel()
        searchJob = null
        isSearchOpen.value = false
        searchQuery.value = ""
        searchResults.value = CatalogShelf(title = "Search", isLoading = false)
        searchShelves.value = emptyList()
    }

    fun openDetails(item: CatalogItem) {
        detailsJob?.cancel()
        selectedDetails.value = MetaDetails(item = item, isLoading = true)
        detailsJob = viewModelScope.launch {
            catalogRepository.getMetaDetailsFlow(type = item.type, id = item.id, videoId = null, guessStreamPath = false)
                .collect { details ->
                    val metaItem = details.metaItem
                    if (metaItem == null) {
                        selectedDetails.value = MetaDetails(item = item, isLoading = true)
                        return@collect
                    }
                    when (val content = metaItem.content) {
                        is com.stremio.core.models.LoadableMetaItem.Content.Loading -> {
                            selectedDetails.value = MetaDetails(item = item, isLoading = true)
                        }
                        is com.stremio.core.models.LoadableMetaItem.Content.Error -> {
                            selectedDetails.value = MetaDetails(
                                item = item,
                                isLoading = false,
                                error = content.value.message,
                            )
                        }
                        is com.stremio.core.models.LoadableMetaItem.Content.Ready -> {
                            val meta = content.value
                            val detailsItem = CatalogItem(
                                id = meta.id,
                                type = meta.type,
                                name = meta.name,
                                poster = meta.poster ?: item.poster,
                                background = meta.background ?: item.background,
                                releaseInfo = meta.releaseInfo ?: item.releaseInfo,
                                imdbRating = item.imdbRating,
                                inCinema = item.inCinema,
                                watched = meta.watched
                            )
                            val genres = meta.links.filter {
                                it.category.equals("genre", ignoreCase = true) || it.category.equals("genres", ignoreCase = true)
                            }.map { it.name }
                            val cast = meta.links.filter {
                                it.category.equals("cast", ignoreCase = true) || it.category.equals("actor", ignoreCase = true)
                            }.map { it.name }.take(8)
                            val director = meta.links.filter {
                                it.category.equals("director", ignoreCase = true) || it.category.equals("directors", ignoreCase = true)
                            }.map { it.name }.take(4)
                            val trailer = meta.trailerStreams.firstOrNull()?.let { catalogRepository.directUrl(it) }

                            selectedDetails.value = MetaDetails(
                                item = detailsItem,
                                description = meta.description,
                                genres = genres,
                                cast = cast,
                                director = director,
                                runtime = meta.runtime,
                                year = meta.releaseInfo ?: detailsItem.releaseInfo,
                                trailer = trailer,
                                isLoading = false,
                            )
                        }
                        else -> {
                            selectedDetails.value = MetaDetails(item = item, isLoading = true)
                        }
                    }
                }
        }
    }

    fun closeDetails() {
        detailsJob?.cancel()
        detailsJob = null
        selectedDetails.value = null
    }

    fun toggleLibrary(item: CatalogItem) {
        val inLibrary = library.value.items.any { it.id == item.id && it.type == item.type }
        if (inLibrary) {
            catalogRepository.removeFromLibrary(item.id)
            com.posthog.PostHog.capture(
                event = "Library Item Removed",
                properties = mapOf(
                    "item_type" to item.type,
                    "hashed_item_id" to sha256(item.id)
                )
            )
        } else {
            catalogRepository.addToLibrary(item)
            com.posthog.PostHog.capture(
                event = "Library Item Added",
                properties = mapOf(
                    "item_type" to item.type,
                    "hashed_item_id" to sha256(item.id)
                )
            )
        }
    }

    private fun observeAddons() {
        viewModelScope.launch {
            addonRepository.getAddonsFlow().collect { raw ->
                addons.value = addonRepository.extractAddonsUiState(raw)
            }
        }
    }

    fun loadInstalledAddons(type: String? = null) {
        addonRepository.loadInstalledAddons(type)
    }

    /** Applies a filter taken from `state.addons.selectableTypes`/`selectableCatalogs`. */
    fun selectAddonsFilter(request: com.stremio.core.types.addon.ResourceRequest) {
        addonRepository.selectFilter(request)
    }

    fun installAddon(item: AddonItem) {
        addonRepository.installAddon(item)
        com.posthog.PostHog.capture(
            event = "Addon Installed",
            properties = mapOf(
                "addon_id" to sha256(item.id),
                "addon_name" to item.name,
                "addon_version" to (item.version ?: "unknown"),
                "official" to item.official
            )
        )
    }

    fun uninstallAddon(item: AddonItem) {
        addonRepository.uninstallAddon(item)
        com.posthog.PostHog.capture(
            event = "Addon Uninstalled",
            properties = mapOf(
                "addon_id" to sha256(item.id),
                "addon_name" to item.name,
                "addon_version" to (item.version ?: "unknown"),
                "official" to item.official
            )
        )
    }

    fun upgradeAddon(item: AddonItem) {
        addonRepository.upgradeAddon(item)
        com.posthog.PostHog.capture(
            event = "Addon Upgraded",
            properties = mapOf(
                "addon_id" to sha256(item.id),
                "addon_name" to item.name,
                "addon_version" to (item.version ?: "unknown"),
                "official" to item.official
            )
        )
    }

    fun openAddonDetails(transportUrl: String) {
        addonDetailsJob?.cancel()
        selectedAddonDetails.value = AddonDetailsUiState(transportUrl = transportUrl)
        addonDetailsJob = viewModelScope.launch {
            addonRepository.getAddonDetailsFlow(transportUrl).collect { details ->
                addonRepository.extractAddonDetailsUiState(details)?.let { selectedAddonDetails.value = it }
            }
        }
    }

    fun closeAddonDetails() {
        addonDetailsJob?.cancel()
        addonDetailsJob = null
        selectedAddonDetails.value = null
    }

    fun installAddonByUrl(rawUrl: String): Boolean {
        val url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val isUrl = runCatching { URL(url) }.isSuccess
        if (isUrl) {
            com.posthog.PostHog.capture(
                event = "Custom Addon Detail Opened",
                properties = mapOf(
                    "hashed_url" to sha256(url)
                )
            )
            openAddonDetails(url)
        }
        return isUrl
    }

    fun login(email: String, password: String) {
        authInFlight = true
        account.value = account.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { authRepository.login(email, password) }.onFailure {
                authInFlight = false
                account.value = account.value.copy(isLoading = false, error = it.message ?: "Login failed")
            }
        }
    }

    fun loginWithFacebook(token: String) {
        authInFlight = true
        account.value = account.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { authRepository.loginWithFacebook(token) }.onFailure {
                authInFlight = false
                account.value = account.value.copy(isLoading = false, error = it.message ?: "Facebook login failed")
            }
        }
    }

    fun signup(email: String, password: String, marketingConsent: Boolean) {
        authInFlight = true
        account.value = account.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { authRepository.register(email, password, marketingConsent) }.onFailure {
                authInFlight = false
                account.value = account.value.copy(isLoading = false, error = it.message ?: "Sign up failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { runCatching { authRepository.logout() } }
        authRepository.clearSavedSession()
        account.value = AccountUiState()
        refreshLibrary()
        com.posthog.PostHog.reset()
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId("")
    }

    fun clearAccountError() {
        account.value = account.value.copy(error = null)
    }

    fun setAccountError(message: String) {
        authInFlight = false
        account.value = account.value.copy(isLoading = false, error = message)
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            try {
                catalogRepository.syncLibrary()
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync library")
            }
        }
    }

    fun selectLibraryFilter(request: com.stremio.core.models.LibraryWithFilters.LibraryRequest) {
        viewModelScope.launch {
            try {
                catalogRepository.loadLibrary(request)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load library request")
            }
        }
    }

    fun selectDiscoverFilter(request: com.stremio.core.types.addon.ResourceRequest) {
        discoverCatalogRequest.value = request
        discoverCatalog.value = discoverCatalog.value.copy(isLoading = true, seeAllRequest = request)
        viewModelScope.launch {
            try {
                catalogRepository.loadDiscover(request)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load discover request")
            }
        }
    }

    private fun observeAddonSubtitlesForPlayback() {
        subtitleObserverJob?.cancel()
        val seen = mutableSetOf<String>()
        subtitleObserverJob = viewModelScope.launch {
            playbackRepository.playerFlow().collect { player ->
                val newTracks = playbackRepository.extractAddonSubtitles(player)
                    .filter { seen.add(it.id) }
                if (newTracks.isNotEmpty()) {
                    playbackRepository.addExternalSubtitleTracks(newTracks)
                }
            }
        }
    }

    private fun copyLocalSubtitle(uri: Uri): ExternalSubtitle? {
        val displayName = queryDisplayName(uri) ?: "subtitle-${System.currentTimeMillis()}.srt"
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outDir = File(appContext.cacheDir, "local-subtitles").apply { mkdirs() }
        val outFile = File(outDir, "${System.currentTimeMillis()}-$safeName")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        return ExternalSubtitle(
            id = "local:${outFile.name}",
            lang = LanguageCatalog.LOCAL_SUBTITLES_LANGUAGE,
            url = Uri.fromFile(outFile).toString(),
            label = displayName,
            source = "Local",
            origin = "LOCAL",
            embedded = false,
            local = true,
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
    }

    private fun sha256(input: String): String {
        val bytes = input.lowercase().trim().toByteArray(Charsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
