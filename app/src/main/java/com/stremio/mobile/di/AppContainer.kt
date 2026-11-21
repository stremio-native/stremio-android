package com.stremio.mobile.di

import android.content.Context
import timber.log.Timber
import com.stremio.mobile.core.StremioCore
import com.stremio.mobile.player.PlaybackManager
import com.stremio.mobile.server.StubStreamingServerController
import com.stremio.mobile.server.StreamingServerController
import com.stremio.mobile.server.JniStreamingServerController
import com.stremio.mobile.server.RemoteStreamingServerController

import com.stremio.mobile.data.repository.AddonRepository
import com.stremio.mobile.data.repository.AuthRepository
import com.stremio.mobile.data.repository.BoardRepository
import com.stremio.mobile.data.repository.CatalogRepository
import com.stremio.mobile.data.repository.PlaybackRepository
import com.stremio.mobile.update.ApkInstaller
import com.stremio.mobile.update.UpdateRepository

class AppContainer(context: Context) {
    val playbackManager = PlaybackManager(context.applicationContext)
    val core: StremioCore = StremioCore(context.applicationContext).apply {
        try {
            initialize()
        } catch (e: Throwable) {
            Timber.e(e, "Failed to initialize stremio-core")
        }
    }

    val authRepository = AuthRepository(core, context.applicationContext)
    val boardRepository = BoardRepository(core)
    val catalogRepository = CatalogRepository(core)
    val addonRepository = AddonRepository(core)
    val playbackRepository = PlaybackRepository(core, playbackManager)
    val updateRepository = UpdateRepository(context.applicationContext)
    val apkInstaller = ApkInstaller(context.applicationContext)

    val serverController: StreamingServerController = createServerController(context)

    private fun createServerController(context: Context): StreamingServerController {
        val customUrl = authRepository.getActiveCustomStreamingServerUrl()
        if (customUrl != null) {
            core.streamingServerUrl = customUrl
            return RemoteStreamingServerController(customUrl)
        }
        return try {
            JniStreamingServerController(context, useForegroundService = { authRepository.isServerInForeground() })
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(e, "Failed to load stream_server JNI, falling back to Stub")
            StubStreamingServerController()
        }
    }

}
