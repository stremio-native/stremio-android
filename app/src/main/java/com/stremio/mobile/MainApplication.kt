package com.stremio.mobile

import android.app.Application
import android.content.Context
import com.stremio.mobile.di.AppContainer
import coil3.SingletonImageLoader
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import okhttp3.Dns
import java.net.InetAddress
import okio.Path.Companion.toPath

import timber.log.Timber
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class CrashlyticsTree : Timber.Tree() {
    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val passwordRegex = Regex("(?i)(password|token|apiKey|api_key|secret|auth|bearer|pass)\\s*[:=]\\s*[^&\\s]+")
    private val bearerRegex = Regex("(?i)Bearer\\s+[^&\\s]+")

    @Volatile
    private var hostRegex: Regex? = null

    /** Keeps [host] - the user's own remote streaming server (LAN/Tailscale address) - out of crash reports. */
    fun redactHost(host: String) {
        val escaped = Regex.escape(host.removeSurrounding("[", "]"))
        // Whole host only: "pc" must not match inside "pcm", but may end a sentence ("... pc.").
        hostRegex = Regex("""(?<![\w.-])$escaped(?![\w-]|\.\w)""", RegexOption.IGNORE_CASE)
    }

    private fun sanitize(message: String): String {
        var sanitized = message
        sanitized = emailRegex.replace(sanitized, "[EMAIL_REDACTED]")
        sanitized = passwordRegex.replace(sanitized, "$1=[REDACTED]")
        sanitized = bearerRegex.replace(sanitized, "Bearer [REDACTED]")
        hostRegex?.let { sanitized = it.replace(sanitized, "[HOST_REDACTED]") }
        return sanitized
    }

    /** Sanitizes the message of [t] and of its causes, re-wrapping only the throwables whose message changed. */
    private fun sanitizeThrowable(t: Throwable): Throwable {
        val cause = t.cause?.takeIf { it !== t }?.let(::sanitizeThrowable)
        val message = t.message?.let(::sanitize)
        if (message == t.message && cause === t.cause) return t
        return Throwable(message, cause).also { it.stackTrace = t.stackTrace }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
            return
        }
        val crashlytics = FirebaseCrashlytics.getInstance()
        val sanitizedMessage = sanitize((tag?.let { "[$it] " } ?: "") + message)
        crashlytics.log(sanitizedMessage)
        if (t != null) {
            crashlytics.recordException(sanitizeThrowable(t))
        }
    }
}

class MainApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        val crashlyticsTree = CrashlyticsTree()
        Timber.plant(crashlyticsTree)
        try {
            android.system.Os.setenv("TMPDIR", cacheDir.absolutePath, true)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set TMPDIR environment variable")
        }
        container = AppContainer(this)
        container.customServerUrl?.let { crashlyticsTree.redactHost(java.net.URI(it).host) }
        val analyticsEnabled = container.authRepository.isAnalyticsEnabled()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(analyticsEnabled)

        if (BuildConfig.POSTHOG_API_KEY.isNotEmpty()) {
            val posthogConfig = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST
            ).apply {
                captureApplicationLifecycleEvents = true
                captureScreenViews = true
                sessionReplay = false
                sessionReplayConfig.maskAllTextInputs = true
                sessionReplayConfig.maskAllImages = true
                optOut = !analyticsEnabled
            }
            PostHogAndroid.setup(this, posthogConfig)
        }

    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(1024L * 1024L * 250L) // 250 MB
                    .build()
            }
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .dns(object : Dns {
                                    override fun lookup(hostname: String): List<InetAddress> {
                                        return Dns.SYSTEM.lookup(hostname).sortedBy { it is java.net.Inet6Address }
                                    }
                                })
                                .build()
                        }
                    )
                )
            }
            .build()
    }
}
