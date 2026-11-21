package com.stremio.mobile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stremio.mobile.auth.FacebookLoginBridge
import com.stremio.mobile.presentation.screens.StremioMobileApp
import com.stremio.mobile.presentation.viewmodel.MainViewModel
import com.stremio.mobile.server.LOCAL_NETWORK_PERMISSION

class MainActivity : ComponentActivity() {
    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[LOCAL_NETWORK_PERMISSION] == true) viewModel.checkServerWorking()
    }

    private val viewModel: MainViewModel by viewModels {
        val app = application as MainApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    authRepository = app.container.authRepository,
                    boardRepository = app.container.boardRepository,
                    catalogRepository = app.container.catalogRepository,
                    addonRepository = app.container.addonRepository,
                    playbackRepository = app.container.playbackRepository,
                    updateRepository = app.container.updateRepository,
                    apkInstaller = app.container.apkInstaller,
                    serverController = app.container.serverController,
                    core = app.container.core,
                    appContext = app.applicationContext,
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.sessionRestoring.value
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestPermissions()
        viewModel.acceptIntent(intent)

        setContent {
            StremioMobileApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.acceptIntent(intent)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (FacebookLoginBridge.callbackManager.onActivityResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }

    private fun requestPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 37 && (application as MainApplication).container.customServerUrl != null) {
                add(LOCAL_NETWORK_PERMISSION)
            }
        }
        if (permissions.isNotEmpty()) permissionRequest.launch(permissions.toTypedArray())
    }
}
