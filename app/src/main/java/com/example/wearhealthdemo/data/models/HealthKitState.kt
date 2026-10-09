package com.example.wearhealthdemo.data.models

import com.example.wearhealthdemo.healthkit.HealthKitAuthMethod
import com.example.wearhealthdemo.healthkit.HealthKitConfig
import java.util.concurrent.TimeUnit

data class HealthKitState(
    val authMethod: HealthKitAuthMethod = HealthKitConfig.defaultAuthMethod,
    val isAuthorized: Boolean = false,
    val isAuthorizing: Boolean = false,
    val isLoadingWorkouts: Boolean = false,
    val workouts: List<WorkoutRecord> = emptyList(),
    val lastSyncedAtMillis: Long? = null,
    val errorMessage: String? = null,
    val needsPrivacy: Boolean = false,
    val accountOpenId: String? = null,
    val lookbackDays: Long = HealthKitConfig.DEFAULT_LOOKBACK_DAYS,
) {
    val rangeStartMillis: Long
        get() = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lookbackDays)
}
