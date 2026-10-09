package com.example.wearhealthdemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.wearhealthdemo.data.models.WorkoutRecord
import com.example.wearhealthdemo.ui.findActivity
import com.example.wearhealthdemo.ui.screens.components.HealthKitCard
import com.example.wearhealthdemo.ui.screens.components.LogCard
import com.example.wearhealthdemo.ui.screens.components.WorkoutDetailDialog
import com.example.wearhealthdemo.viewmodels.MainViewModel

@Composable
fun HealthKitScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf("") }
    var detailWorkout by remember { mutableStateOf<WorkoutRecord?>(null) }
    var askBeforeDeleting by remember { mutableStateOf(false) }

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
            HealthKitCard(
                healthKitState = uiState.healthKitState,
                onConnectClicked = {
                    val activity = context.findActivity()
                    if (activity == null) {
                        toastMessage = "Cannot start authorization from this screen"
                    } else {
                        viewModel.connectHealthKit(activity)
                    }
                },
                onFetchWorkoutsClicked = viewModel::fetchRecentWorkouts,
                onDisconnectClicked = {
                    viewModel.disconnectHealthKit()
                    toastMessage = "Disconnected Huawei Health"
                },
                onDeleteDataClicked = { askBeforeDeleting = true },
                onOpenHuaweiHealthClicked = {
                    val activity = context.findActivity()
                    if (activity == null) {
                        toastMessage = "Cannot open Huawei Health from here"
                    } else {
                        viewModel.openHuaweiHealth(activity)
                    }
                },
                onAuthMethodSelected = viewModel::selectAuthMethod,
                onLookbackSelected = viewModel::selectLookbackDays,
                onWorkoutClicked = { detailWorkout = it },
            )
        }

        item {
            LogCard(
                logMessages = uiState.logMessages,
                onClearLogs = viewModel::clearLogs,
            )
        }
    }

    detailWorkout?.let { workout ->
        WorkoutDetailDialog(workout = workout, onDismiss = { detailWorkout = null })
    }

    if (askBeforeDeleting) {
        AlertDialog(
            onDismissRequest = { askBeforeDeleting = false },
            title = { Text("Delete everything this app wrote") },
            text = {
                Text(
                    "Health Kit Cloud deletes every activity record this app created for " +
                        "this account, right away instead of after three days, and the " +
                        "authorization is cancelled with it. Workouts the watch and Huawei " +
                        "Health wrote are not touched. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        askBeforeDeleting = false
                        viewModel.deleteHealthKitData()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { askBeforeDeleting = false }) { Text("Cancel") }
            },
        )
    }
}
