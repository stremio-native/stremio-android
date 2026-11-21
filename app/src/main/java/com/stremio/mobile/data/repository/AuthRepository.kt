package com.stremio.mobile.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.stremio.mobile.core.StremioCore
import com.stremio.mobile.data.model.LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA
import com.stremio.mobile.data.model.LocalStreamSelection
import com.stremio.mobile.data.model.LiquidGlassTuning
import com.stremio.mobile.data.model.StreamOption
import com.stremio.mobile.core.theme.AppFont
import com.stremio.mobile.presentation.state.AccountUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class AuthRepository(
    private val core: StremioCore,
    private val context: Context
) {
    private val preferences = context.getSharedPreferences("stremio_account", Context.MODE_PRIVATE)

    // Not in "stremio_account", which logout clears.
    private val serverPreferences = context.getSharedPreferences("streaming_server", Context.MODE_PRIVATE)

    fun getSelectedFont(): AppFont {
        val name = preferences.getString("selected_font", AppFont.PLUS_JAKARTA_SANS.name) ?: AppFont.PLUS_JAKARTA_SANS.name
        return runCatching { AppFont.valueOf(name) }.getOrDefault(AppFont.PLUS_JAKARTA_SANS)
    }

    fun setSelectedFont(font: AppFont) {
        preferences.edit().putString("selected_font", font.name).apply()
    }


    fun getAccountStateFlow(): Flow<AccountUiState> {
        return core.ctx().map { ctx ->
            val auth = ctx.profile.auth
            if (auth != null) {
                AccountUiState(isAuthenticated = true, email = auth.user.email, authKey = auth.key)
            } else {
                AccountUiState()
            }
        }
    }

    fun getProfileSettingsFlow() = core.ctx().map { it.profile.settings }

    fun getTraktAuthFlow() = core.ctx().map { ctx ->
        val auth = ctx.profile.auth
        if (auth != null) {
            val trakt = auth.user.trakt
            if (trakt != null) {
                val expiresAtSeconds = trakt.expiresIn.seconds
                val createdAtSeconds = trakt.createdAt.seconds
                (System.currentTimeMillis() / 1000) < (createdAtSeconds + expiresAtSeconds)
            } else {
                false
            }
        } else {
            false
        }
    }

    fun accountFromCore(): AccountUiState {
        val auth = runCatching { core.getCtx().profile.auth }.getOrNull()
        return if (auth != null) {
            AccountUiState(isAuthenticated = true, email = auth.user.email, authKey = auth.key)
        } else {
            AccountUiState()
        }
    }

    fun isAutoStartOnBoot(): Boolean = preferences.getBoolean("auto_start_on_boot", false)
    fun setAutoStartOnBoot(enabled: Boolean) {
        preferences.edit().putBoolean("auto_start_on_boot", enabled).apply()
    }

    fun isServerInForeground(): Boolean = preferences.getBoolean("server_in_foreground", true)
    fun setServerInForeground(enabled: Boolean) {
        preferences.edit().putBoolean("server_in_foreground", enabled).commit()
    }

    fun getCustomStreamingServerUrl(): String? = serverPreferences.getString("custom_streaming_server_url", null)
    fun isCustomStreamingServerEnabled(): Boolean = serverPreferences.getBoolean("custom_streaming_server_enabled", false)
    fun setCustomStreamingServer(enabled: Boolean, url: String?) {
        serverPreferences.edit()
            .putBoolean("custom_streaming_server_enabled", enabled)
            .putString("custom_streaming_server_url", url)
            .commit()
    }

    fun getActiveCustomStreamingServerUrl(): String? = getCustomStreamingServerUrl()?.takeIf { isCustomStreamingServerEnabled() }

    fun isMobileDataWarning(): Boolean = preferences.getBoolean("mobile_data_warning", true)
    fun setMobileDataWarning(enabled: Boolean) {
        preferences.edit().putBoolean("mobile_data_warning", enabled).apply()
    }

    fun isKeepScreenOn(): Boolean = preferences.getBoolean("keep_screen_on", true)
    fun setKeepScreenOn(enabled: Boolean) {
        preferences.edit().putBoolean("keep_screen_on", enabled).apply()
    }

    fun isAnalyticsEnabled(): Boolean = preferences.getBoolean("analytics_enabled", true)
    fun setAnalyticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("analytics_enabled", enabled).apply()
    }

    fun isAnalyticsDisclosureAcknowledged(): Boolean = preferences.getBoolean("analytics_disclosure_acknowledged", false)
    fun setAnalyticsDisclosureAcknowledged(acknowledged: Boolean) {
        preferences.edit().putBoolean("analytics_disclosure_acknowledged", acknowledged).apply()
    }

    fun isAutoUpdateEnabled(): Boolean = preferences.getBoolean("auto_update_enabled", true)
    fun setAutoUpdateEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("auto_update_enabled", enabled).apply()
    }

    fun getLastUpdateCheckMs(): Long = preferences.getLong("last_update_check_ms", 0L)
    fun setLastUpdateCheckMs(value: Long) {
        preferences.edit().putLong("last_update_check_ms", value).apply()
    }

    fun getIgnoredUpdateVersion(): String? = preferences.getString("ignored_update_version", null)
    fun setIgnoredUpdateVersion(value: String?) {
        preferences.edit().putString("ignored_update_version", value).apply()
    }

    fun getMinSeedsThreshold(): Int = preferences.getInt("min_seeds_threshold", 1)
    fun setMinSeedsThreshold(value: Int) {
        preferences.edit().putInt("min_seeds_threshold", value).apply()
    }

    fun getMinDownloadSpeedBps(): Long = preferences.getLong("min_download_speed_bps", 50_000L)
    fun setMinDownloadSpeedBps(value: Long) {
        preferences.edit().putLong("min_download_speed_bps", value).apply()
    }

    fun getPreferredQuality(): String = preferences.getString("preferred_quality", "Any") ?: "Any"
    fun setPreferredQuality(value: String) {
        preferences.edit().putString("preferred_quality", value).apply()
    }

    fun rememberLocalStreamSelection(itemType: String, itemId: String, videoId: String?, option: StreamOption) {
        val payload = JSONObject()
            .put("key", option.key)
            .put("addonTitle", option.addonTitle)
            .put("name", option.name)
            .put("description", option.description)
            .put("quality", option.quality)

        preferences.edit()
            .putString(localStreamSelectionKey(itemType, itemId, videoId), payload.toString())
            .apply()
    }

    fun getLocalStreamSelection(itemType: String, itemId: String, videoId: String?): LocalStreamSelection? {
        val raw = preferences.getString(localStreamSelectionKey(itemType, itemId, videoId), null) ?: return null
        return runCatching {
            val payload = JSONObject(raw)
            LocalStreamSelection(
                key = payload.optString("key"),
                addonTitle = payload.optString("addonTitle"),
                name = payload.optString("name"),
                description = payload.optNullableString("description"),
                quality = payload.optNullableString("quality"),
            )
        }.getOrNull()
    }

    private fun localStreamSelectionKey(itemType: String, itemId: String, videoId: String?): String {
        val normalizedVideoId = videoId?.takeIf { it.isNotBlank() } ?: itemId
        return listOf("local_stream_selection", itemType, itemId, normalizedVideoId)
            .joinToString(":") { Uri.encode(it) }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return if (has(name) && !isNull(name)) optString(name) else null
    }

    fun getGlobalUiStyle(): String {
        val value = preferences.getString("global_ui_style", null)
            ?: preferences.getString("player_controls_style", null)
        return when (value) {
            "modern" -> "modern"
            else -> "classic"
        }
    }
    fun setGlobalUiStyle(value: String) {
        preferences.edit().putString("global_ui_style", if (value == "modern") "modern" else "classic").apply()
    }

    fun getPlayerUiStyle(): String {
        return preferences.getString("player_ui_style", "global") ?: "global"
    }

    fun setPlayerUiStyle(value: String) {
        val resolved = when (value) {
            "classic" -> "classic"
            "modern" -> "modern"
            else -> "global"
        }
        preferences.edit().putString("player_ui_style", resolved).apply()
    }


    fun getGlassEffectsMode(): String {
        return when (preferences.getString("glass_effects_mode", "balanced")) {
            "full" -> "full"
            "static" -> "static"
            else -> "balanced"
        }
    }
    fun setGlassEffectsMode(value: String) {
        val resolved = when (value) {
            "full" -> "full"
            "static" -> "static"
            else -> "balanced"
        }
        preferences.edit().putString("glass_effects_mode", resolved).apply()
    }

    fun getGlobalGlassAlpha(): Float = preferences.getFloat("global_glass_alpha", LIQUID_GLASS_RECOMMENDED_GLOBAL_ALPHA)
    fun setGlobalGlassAlpha(value: Float) {
        preferences.edit().putFloat("global_glass_alpha", value).apply()
    }

    fun getGlassHapticsEnabled(): Boolean = preferences.getBoolean("glass_haptics_enabled", true)
    fun setGlassHapticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("glass_haptics_enabled", enabled).apply()
    }

    fun isAdaptiveGlassContrastEnabled(): Boolean = preferences.getBoolean("adaptive_glass_contrast", true)
    fun setAdaptiveGlassContrastEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("adaptive_glass_contrast", enabled).apply()
    }

    fun getHapticsIntensity(): String = preferences.getString("haptics_intensity", "Medium") ?: "Medium"
    fun setHapticsIntensity(value: String) {
        preferences.edit().putString("haptics_intensity", value).apply()
    }

    fun getLiquidGlassTuning(): LiquidGlassTuning {
        val defaults = LiquidGlassTuning()
        return LiquidGlassTuning(
            blurDp = preferences.getFloat("glass_tuning_blur_dp", defaults.blurDp),
            refractionHeightDp = preferences.getFloat("glass_tuning_refraction_height_dp", defaults.refractionHeightDp),
            refractionAmountDp = preferences.getFloat("glass_tuning_refraction_amount_dp", defaults.refractionAmountDp),
            surfaceAlpha = preferences.getFloat("glass_tuning_surface_alpha", defaults.surfaceAlpha),
            highlightAlpha = preferences.getFloat("glass_tuning_highlight_alpha", defaults.highlightAlpha),
            borderAlpha = preferences.getFloat("glass_tuning_border_alpha", defaults.borderAlpha),
            shadowAlpha = preferences.getFloat("glass_tuning_shadow_alpha", defaults.shadowAlpha),
            trackAlpha = preferences.getFloat("glass_tuning_track_alpha", defaults.trackAlpha),
            thumbAlpha = preferences.getFloat("glass_tuning_thumb_alpha", defaults.thumbAlpha),
            chromaticAberration = preferences.getBoolean("glass_tuning_chromatic_aberration", defaults.chromaticAberration),
        ).clamped()
    }

    fun setLiquidGlassTuning(value: LiquidGlassTuning) {
        val tuning = value.clamped()
        preferences.edit()
            .putFloat("glass_tuning_blur_dp", tuning.blurDp)
            .putFloat("glass_tuning_refraction_height_dp", tuning.refractionHeightDp)
            .putFloat("glass_tuning_refraction_amount_dp", tuning.refractionAmountDp)
            .putFloat("glass_tuning_surface_alpha", tuning.surfaceAlpha)
            .putFloat("glass_tuning_highlight_alpha", tuning.highlightAlpha)
            .putFloat("glass_tuning_border_alpha", tuning.borderAlpha)
            .putFloat("glass_tuning_shadow_alpha", tuning.shadowAlpha)
            .putFloat("glass_tuning_track_alpha", tuning.trackAlpha)
            .putFloat("glass_tuning_thumb_alpha", tuning.thumbAlpha)
            .putBoolean("glass_tuning_chromatic_aberration", tuning.chromaticAberration)
            .apply()
    }

    fun isAutoSwitchOnDeadStream(): Boolean = preferences.getBoolean("auto_switch_on_dead_stream", false)
    fun setAutoSwitchOnDeadStream(enabled: Boolean) {
        preferences.edit().putBoolean("auto_switch_on_dead_stream", enabled).apply()
    }

    fun isTraktAuthenticated(): Boolean {
        val user = runCatching { core.getCtx().profile.auth?.user }.getOrNull() ?: return false
        val trakt = user.trakt ?: return false
        val expiresAtSeconds = trakt.expiresIn.seconds
        val createdAtSeconds = trakt.createdAt.seconds
        return (System.currentTimeMillis() / 1000) < (createdAtSeconds + expiresAtSeconds)
    }

    fun authenticateTrakt(context: Context) {
        val user = runCatching { core.getCtx().profile.auth?.user }.getOrNull() ?: return
        val url = "https://www.strem.io/trakt/auth/${user.id}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    suspend fun logoutTrakt() {
        core.logoutTrakt()
    }

    suspend fun installTraktAddon() {
        core.installTraktAddon()
    }

    fun getSavedAuthKey(): String? = preferences.getString("authKey", null)

    fun saveAuthKeyAndEmail(authKey: String, email: String) {
        preferences.edit()
            .putString("authKey", authKey)
            .putString("email", email)
            .apply()
    }

    fun clearSavedSession() {
        preferences.edit().clear().apply()
    }

    suspend fun loginWithToken(authKey: String) {
        core.loginWithToken(authKey)
    }

    suspend fun loginWithFacebook(token: String) {
        core.loginWithFacebook(token)
    }

    suspend fun login(email: String, password: String) {
        core.login(email.trim(), password)
    }

    suspend fun register(email: String, password: String, marketingConsent: Boolean) {
        core.register(email.trim(), password, marketingConsent)
    }

    suspend fun logout() {
        core.logout()
    }

    fun isAuthenticated(): Boolean = core.isAuthenticated()

    suspend fun updateSettings(newSettings: com.stremio.core.types.profile.Profile.Settings) {
        core.updateSettings(newSettings)
    }
}
