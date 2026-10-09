package com.example.wearhealthdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.wearhealthdemo.healthkit.HealthKitClient
import com.example.wearhealthdemo.managers.AuthManager
import com.example.wearhealthdemo.managers.DeviceManager
import com.example.wearhealthdemo.managers.P2pManager
import com.example.wearhealthdemo.managers.WatchTypeStore
import com.example.wearhealthdemo.push.WorkoutSubscriptions
import com.example.wearhealthdemo.ui.WearEngineApp
import com.example.wearhealthdemo.ui.theme.WearEngineAndroidLiteFileMessageSenderTheme
import com.example.wearhealthdemo.viewmodels.MainViewModel
import com.example.wearhealthdemo.viewmodels.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var p2pManager: P2pManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        p2pManager = P2pManager(this)

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                authManager = AuthManager(this),
                deviceManager = DeviceManager(this),
                p2pManager = p2pManager,
                healthKitClient = HealthKitClient(this),
                watchTypeStore = WatchTypeStore(this),
                workoutSubscriptions = WorkoutSubscriptions(this),
            ),
        )[MainViewModel::class.java]

        setContent {
            WearEngineAndroidLiteFileMessageSenderTheme {
                WearEngineApp(viewModel = viewModel)
            }
        }

        intent?.data?.let(viewModel::handleHealthKitRedirect)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::handleHealthKitRedirect)
    }

    @Deprecated("Kept because the login-free SDK reports its outcome this way.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleAuthorizationActivityResult(this, requestCode, resultCode, data)
    }

    override fun onDestroy() {
        viewModel.sendAppDestroy(if (isFinishing) "user closed the app" else "activity destroyed")
        p2pManager.unregisterReceiver()
        super.onDestroy()
    }
}
