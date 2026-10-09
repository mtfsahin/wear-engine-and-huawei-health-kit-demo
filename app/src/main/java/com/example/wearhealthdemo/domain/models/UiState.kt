package com.example.wearhealthdemo.domain.models

import androidx.compose.runtime.Immutable
import com.example.wearhealthdemo.data.models.DeviceState
import com.example.wearhealthdemo.data.models.MessageState
import com.example.wearhealthdemo.data.models.FileState
import com.example.wearhealthdemo.data.models.HealthKitState

@Immutable
data class UiState(
    val deviceState: DeviceState = DeviceState(),
    val messageState: MessageState = MessageState(),
    val fileState: FileState = FileState(),
    val healthKitState: HealthKitState = HealthKitState(),
    val receivedMessages: List<String> = emptyList(),
    val logMessages: List<String> = emptyList()
)