package com.example.wearhealthdemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wearhealthdemo.data.models.HealthKitState
import com.example.wearhealthdemo.data.models.WorkoutRecord
import com.example.wearhealthdemo.healthkit.HealthKitAuthMethod
import com.example.wearhealthdemo.ui.findActivity
import com.example.wearhealthdemo.ui.screens.components.LogCard
import com.example.wearhealthdemo.ui.screens.components.PillButton
import com.example.wearhealthdemo.ui.screens.components.ResultCard
import com.example.wearhealthdemo.ui.screens.components.SolutionInfo
import com.example.wearhealthdemo.ui.screens.components.TokenList
import com.example.wearhealthdemo.ui.screens.components.WorkoutDetailDialog
import com.example.wearhealthdemo.ui.screens.components.WorkoutSection
import com.example.wearhealthdemo.viewmodels.MainViewModel

@Composable
fun LoginFreeScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.healthKitState
    val busy = state.isAuthorizing || state.isRefreshingToken || state.isLoadingWorkouts
    var toastMessage by remember { mutableStateOf("") }
    var detailWorkout by remember { mutableStateOf<WorkoutRecord?>(null) }
    var askBeforeDeleting by remember { mutableStateOf(false) }

    // This app only offers the login-free flow
    LaunchedEffect(state.authMethod) {
        if (state.authMethod != HealthKitAuthMethod.LOGIN_FREE_SDK) {
            viewModel.selectAuthMethod(HealthKitAuthMethod.LOGIN_FREE_SDK)
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotBlank()) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            toastMessage = ""
        }
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Login-free",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    viewModel.disconnectHealthKit()
                    viewModel.clearLogs()
                    toastMessage = "Started over"
                },
                enabled = !busy && (state.isAuthorized || state.errorMessage != null),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Start over")
            }
        }

        // Result card scrolls too, error texts can be long
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                val (title, detail) = resultText(state)
                ResultCard(title = title, detail = detail)
            }
            if (state.tokenItems.isEmpty()) {
                item {
                    SolutionInfo(
                        title = "Login-free authorization",
                        description = "Opens the Huawei Health app, asks for the scopes and " +
                            "then for cloud sync if it is off. No HUAWEI ID sign-in screen.",
                    )
                }
            } else {
                item {
                    TokenList(
                        items = state.tokenItems,
                        onCopied = { toastMessage = "Copied" },
                    )
                }
            }

            if (state.isAuthorized) {
                item {
                    WorkoutSection(
                        state = state,
                        onLookbackSelected = viewModel::selectLookbackDays,
                        onFetchWorkoutsClicked = viewModel::fetchRecentWorkouts,
                        onOpenHuaweiHealthClicked = {
                            val activity = context.findActivity()
                            if (activity == null) {
                                toastMessage = "Cannot open Huawei Health from here"
                            } else {
                                viewModel.openHuaweiHealth(activity)
                            }
                        },
                        onDeleteDataClicked = { askBeforeDeleting = true },
                        onWorkoutClicked = { detailWorkout = it },
                    )
                }
            }

            if (uiState.logMessages.isNotEmpty()) {
                item {
                    LogCard(
                        logMessages = uiState.logMessages,
                        onClearLogs = viewModel::clearLogs,
                    )
                }
            }
        }

        PillButton(
            label = "Start login-free",
            filled = true,
            enabled = !busy,
            onClick = {
                val activity = context.findActivity()
                if (activity == null) {
                    toastMessage = "Cannot start authorization from this screen"
                } else {
                    viewModel.connectHealthKit(activity)
                }
            },
        )
        if (state.tokenItems.any { it.first == "refresh_token" && it.second.isNotEmpty() }) {
            PillButton(
                label = "Refresh token",
                filled = false,
                enabled = !busy,
                onClick = viewModel::refreshToken,
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

private fun resultText(state: HealthKitState): Pair<String, String> {
    val expiresIn = state.tokenItems.firstOrNull { it.first == "expires_in" }?.second
    return when {
        state.isAuthorizing -> "Waiting" to "Finish the authorization in Huawei Health."
        state.isRefreshingToken -> "Refreshing" to "Requesting a new access token…"
        state.errorMessage != null && !state.isAuthorized -> "Error" to state.errorMessage
        state.isAuthorized -> "Token received" to (
            state.errorMessage ?: "The access token expires in $expiresIn seconds."
        )
        else -> "Waiting" to "Start login-free authorization below."
    }
}
