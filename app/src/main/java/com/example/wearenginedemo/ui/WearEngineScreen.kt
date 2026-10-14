package com.example.wearenginedemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wearenginedemo.WatchApp
import com.example.wearenginedemo.WearEngineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearEngineScreen(viewModel: WearEngineViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wear Engine demo") },
                actions = { TextButton(onClick = viewModel::clearLog) { Text("Clear") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            item {
                Step("1. Permission") {
                    Text(if (state.hasPermission) "DEVICE_MANAGER is granted." else "The app needs DEVICE_MANAGER.")
                    if (!state.hasPermission) {
                        Button(onClick = viewModel::requestPermission) { Text("Grant permission") }
                    }
                }
            }

            item {
                Step("2. Watch") {
                    if (state.watches.isEmpty()) {
                        Text("No paired watch. Pair one in the Huawei Health app.")
                    }
                    state.watches.forEach { watch ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectWatch(watch) },
                        ) {
                            RadioButton(selected = watch == state.selectedWatch, onClick = { viewModel.selectWatch(watch) })
                            Text(if (watch.isConnected) "${watch.name} (connected)" else watch.name)
                        }
                    }
                    OutlinedButton(onClick = viewModel::loadWatches, enabled = state.hasPermission) {
                        Text("Refresh")
                    }
                }
            }

            item {
                Step("3. Watch app") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        WatchApp.entries.forEachIndexed { index, watchApp ->
                            SegmentedButton(
                                selected = watchApp == state.watchApp,
                                onClick = { viewModel.selectWatchApp(watchApp) },
                                shape = SegmentedButtonDefaults.itemShape(index, WatchApp.entries.size),
                            ) {
                                Text(watchApp.label)
                            }
                        }
                    }
                    Button(onClick = viewModel::ping, enabled = state.selectedWatch != null) { Text("Ping") }
                    if (state.pingResult.isNotEmpty()) {
                        Text("Ping result ${state.pingResult}")
                    }
                }
            }

            item {
                Step("4. Send a message") {
                    OutlinedTextField(
                        value = state.message,
                        onValueChange = viewModel::updateMessage,
                        label = { Text("Message, up to 1 KB") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = viewModel::sendMessage,
                        enabled = state.selectedWatch != null && state.message.isNotBlank(),
                    ) {
                        Text("Send")
                    }
                }
            }

            item {
                Step("5. Messages from the watch") {
                    if (state.receivedMessages.isEmpty()) {
                        Text("Nothing yet")
                    }
                    state.receivedMessages.forEach { Text(it) }
                }
            }

            item {
                Step("Log") {
                    state.log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun Step(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
