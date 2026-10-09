package com.example.wearhealthdemo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wearhealthdemo.healthkit.HealthKitClient
import com.example.wearhealthdemo.managers.AuthManager
import com.example.wearhealthdemo.managers.DeviceManager
import com.example.wearhealthdemo.managers.P2pManager
import com.example.wearhealthdemo.managers.WatchTypeStore
import com.example.wearhealthdemo.push.WorkoutSubscriptions

class MainViewModelFactory(
    private val authManager: AuthManager,
    private val deviceManager: DeviceManager,
    private val p2pManager: P2pManager,
    private val healthKitClient: HealthKitClient,
    private val watchTypeStore: WatchTypeStore,
    private val workoutSubscriptions: WorkoutSubscriptions,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            authManager = authManager,
            deviceManager = deviceManager,
            p2pManager = p2pManager,
            healthKitClient = healthKitClient,
            watchTypeStore = watchTypeStore,
            workoutSubscriptions = workoutSubscriptions,
        ) as T
    }
}
