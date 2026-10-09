package com.example.wearhealthdemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wearhealthdemo.ui.screens.components.LogCard
import com.example.wearhealthdemo.ui.screens.components.MessageFileSenderCard
import com.example.wearhealthdemo.ui.screens.components.ReceivedMessagesCard
import com.example.wearhealthdemo.ui.screens.components.SelectDeviceCard
import com.example.wearhealthdemo.viewmodels.MainViewModel

@Composable
fun WearEngineScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf("") }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotBlank()) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            toastMessage = ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        item {
            SelectDeviceCard(
                deviceState = uiState.deviceState,
                onDeviceSelected = { device ->
                    viewModel.selectDevice(device)
                    toastMessage = "Selected: " + device.name
                },
                onRefreshClicked = {
                    viewModel.refreshDevices()
                    toastMessage = "Refreshing devices..."
                },
            )
        }

        item {
            MessageFileSenderCard(
                viewModel = viewModel,
                messageState = uiState.messageState,
                onMessageChanged = viewModel::updateMessageText,
                onSendMessage = {
                    when {
                        uiState.deviceState.selectedDevice == null ->
                            toastMessage = "Please select a device first"

                        uiState.messageState.text.isBlank() ->
                            toastMessage = "Message cannot be empty"

                        else -> {
                            toastMessage = "Sending message..."
                            viewModel.sendMessage(uiState.messageState.text)
                        }
                    }
                },
                onSendPing = {
                    if (uiState.deviceState.selectedDevice == null) {
                        toastMessage = "Please select a device first"
                    } else {
                        toastMessage = "Sending ping..."
                        viewModel.sendPing()
                    }
                },
                isDeviceSelected = uiState.deviceState.selectedDevice != null,
            )
        }

        item {
            ReceivedMessagesCard(receivedMessages = uiState.receivedMessages)
        }

        item {
            LogCard(
                logMessages = uiState.logMessages,
                onClearLogs = viewModel::clearLogs,
            )
        }
    }
}
