package com.example.wearhealthdemo.data.models

import com.example.wearhealthdemo.healthkit.WorkoutType

data class WorkoutRecord(
    val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val activityType: Int,
    val activeTimeMillis: Long,
    val timeZone: String,
    val sourceType: Int? = null,
    val summary: Map<String, String> = emptyMap(),
) {
    val activityTypeName: String
        get() = WorkoutType.displayName(activityType)

    val durationMinutes: Long
        get() = activeTimeMillis / 1000 / 60

    fun summaryLabel(key: String): String =
        key.removePrefix("com.huawei.")
            .removePrefix("continuous.")
            .replace('.', ' ')
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
}
