package com.stremio.mobile.presentation.state

import com.stremio.mobile.core.theme.AppFont
import com.stremio.mobile.data.model.CatalogShelf
import com.stremio.mobile.data.model.LiquidGlassTuning
import com.stremio.mobile.data.model.LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
import com.stremio.mobile.data.model.MetaDetails
import com.stremio.mobile.server.StreamingServerState
import com.stremio.mobile.update.UpdateState

data class MainUiState(
    val server: StreamingServerState = StreamingServerState.Stopped,
    val selectedFont: AppFont = AppFont.PLUS_JAKARTA_SANS,

    val serverPingStatus: String = "Not checked",
    val isPingLoading: Boolean = false,
    val serverVersion: String = "0.1.8",
    val serverConfigPath: String = "",
    val serverCachePath: String = "",
    val latestIntentUri: String? = null,
    val account: AccountUiState = AccountUiState(),
    val selectedSection: MainSection = MainSection.Home,
    val searchQuery: String = "",
    val searchResults: CatalogShelf = CatalogShelf(title = "Search", isLoading = false),
    val searchShelves: List<CatalogShelf> = emptyList(),
    val library: CatalogShelf = CatalogShelf(title = "Library", isLoading = false),
    val addons: AddonsUiState = AddonsUiState(),
    val selectedAddonDetails: AddonDetailsUiState? = null,
    val nextVideo: com.stremio.core.types.resource.Video? = null,
    val showNextVideoPopup: Boolean = false,
    val minSeedsThreshold: Int = 1,
    val minDownloadSpeedBps: Long = 50_000L,
    val preferredQuality: String = "Any",
    val globalUiStyle: String = "classic",
    val playerUiStyle: String = "global",
    val glassEffectsMode: String = "balanced",
    val isAutoSwitchOnDeadStream: Boolean = false,
    val showNoSeedsBanner: Boolean = false,
    val noSeedsReason: String? = null,
    val selectedDetails: MetaDetails? = null,
    val continueWatching: CatalogShelf = CatalogShelf(title = "Continue Watching"),
    val boardShelves: List<CatalogShelf> = emptyList(),
    val discoverCatalogRequest: com.stremio.core.types.addon.ResourceRequest? = null,
    val discoverCatalogTitle: String? = null,
    val discoverCatalog: CatalogShelf = CatalogShelf(title = "Discover", isLoading = false),
    val discoverCatalogWithFilters: com.stremio.core.models.CatalogWithFilters? = null,
    val libraryWithFilters: com.stremio.core.models.LibraryWithFilters? = null,
    val isDiscoverSeeAll: Boolean = false,
    val profileSettings: com.stremio.core.types.profile.Profile.Settings? = null,
    val serverSettings: com.stremio.core.models.StreamingServer.Settings? = null,
    /** Fixed until the app restarts. */
    val usesRemoteStreamingServer: Boolean = false,
    val customStreamingServerUrl: String = "",
    val isCustomStreamingServerEnabled: Boolean = false,
    val isTraktAuthenticated: Boolean = false,
    val isAutoStartOnBoot: Boolean = false,
    val isServerInForeground: Boolean = true,
    val isMobileDataWarning: Boolean = true,
    val showMobileDataWarning: Boolean = false,
    val isKeepScreenOn: Boolean = true,
    val isAnalyticsEnabled: Boolean = true,
    val showAnalyticsDisclosure: Boolean = false,
    val isSeedingEnabled: Boolean = true,
    val isSearchOpen: Boolean = false,
    val globalGlassAlpha: Float = LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA,
    val adaptiveGlassContrast: Boolean = true,
    val glassHapticsEnabled: Boolean = true,
    val hapticsIntensity: String = "Medium",
    val liquidGlassTuning: LiquidGlassTuning = LiquidGlassTuning(),
    val updateState: UpdateState = UpdateState.Idle,
    val isAutoUpdateEnabled: Boolean = true,
)
