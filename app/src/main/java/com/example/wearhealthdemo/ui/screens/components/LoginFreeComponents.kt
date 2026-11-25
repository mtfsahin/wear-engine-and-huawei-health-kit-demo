package com.example.wearhealthdemo.ui.screens.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wearhealthdemo.data.models.HealthKitState
import com.example.wearhealthdemo.data.models.WorkoutRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(22.dp)
private val ListShape = RoundedCornerShape(16.dp)
private val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun ResultCard(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Last result",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            style = TabularNumbers,
        )
        Text(
            text = detail,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SolutionInfo(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(
            text = description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Long press copies a value, like the iOS and Flutter samples
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TokenList(items: List<Pair<String, String>>, onCopied: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ListShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
    ) {
        items.forEachIndexed { index, (name, value) ->
            if (index > 0) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .combinedClickable(
                        enabled = value.isNotEmpty(),
                        onClick = {},
                        onLongClick = {
                            clipboard.setText(AnnotatedString(value))
                            onCopied()
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = value.ifEmpty { "nil" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = TabularNumbers,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun PillButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = when {
            pressed -> 0.3f
            enabled -> 1f
            else -> 0.4f
        },
        label = "pillAlpha",
    )
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .alpha(alpha)
        .clip(CircleShape)
    val shaped = if (filled) {
        base.background(MaterialTheme.colorScheme.onSurface)
    } else {
        base.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
    }
    Box(
        modifier = shaped.clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (filled) MaterialTheme.colorScheme.surface else contentColor,
        )
    }
}

@Composable
fun WorkoutSection(
    state: HealthKitState,
    onLookbackSelected: (Long) -> Unit,
    onFetchWorkoutsClicked: () -> Unit,
    onOpenHuaweiHealthClicked: () -> Unit,
    onDeleteDataClicked: () -> Unit,
    onWorkoutClicked: (WorkoutRecord) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Workouts", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        RangePicker(state = state, onLookbackSelected = onLookbackSelected)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ListShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp),
        ) {
            WorkoutList(
                workouts = state.workouts,
                isLoading = state.isLoadingWorkouts,
                onWorkoutClicked = onWorkoutClicked,
            )
        }

        state.accountOpenId?.let { openId ->
            Text(
                text = "Signed in as HUAWEI ID $openId",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PillButton(
            label = if (state.isLoadingWorkouts) "Fetching…" else "Fetch workouts",
            filled = false,
            enabled = !state.isLoadingWorkouts,
            onClick = onFetchWorkoutsClicked,
        )
        if (state.needsPrivacy) {
            PillButton(
                label = "Open Huawei Health",
                filled = false,
                onClick = onOpenHuaweiHealthClicked,
            )
        }
        PillButton(
            label = "Delete everything this app wrote",
            filled = false,
            enabled = !state.isLoadingWorkouts,
            onClick = onDeleteDataClicked,
            contentColor = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun WorkoutList(
    workouts: List<WorkoutRecord>,
    isLoading: Boolean,
    onWorkoutClicked: (WorkoutRecord) -> Unit,
) {
    if (workouts.isEmpty()) {
        Box(modifier = Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isLoading) "Fetching…" else "No workouts found in this range yet.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    workouts.take(MAX_VISIBLE_WORKOUTS).forEachIndexed { index, workout ->
        if (index > 0) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
        WorkoutRow(workout, onWorkoutClicked)
    }
    if (workouts.size > MAX_VISIBLE_WORKOUTS) {
        Text(
            text = "+ ${workouts.size - MAX_VISIBLE_WORKOUTS} more",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun WorkoutRow(workout: WorkoutRecord, onClick: (WorkoutRecord) -> Unit) {
    val dateFormat = remember(workout.startTime) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick(workout) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = workout.activityTypeName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                text = dateFormat.format(Date(workout.startTime)),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${workout.durationMinutes} min",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TabularNumbers,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePicker(state: HealthKitState, onLookbackSelected: (Long) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LOOKBACK_CHOICES.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = state.lookbackDays == choice.days,
                    onClick = { onLookbackSelected(choice.days) },
                    enabled = !state.isLoadingWorkouts,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = LOOKBACK_CHOICES.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.onSurface,
                        activeContentColor = MaterialTheme.colorScheme.surface,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    icon = {},
                ) {
                    Text(choice.label, fontSize = 14.sp)
                }
            }
        }

        Text(
            text = "${dateFormat.format(Date(state.rangeStartMillis))} – today",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class LookbackChoice(val days: Long, val label: String)

private val LOOKBACK_CHOICES = listOf(
    LookbackChoice(7L, "7 d"),
    LookbackChoice(30L, "30 d"),
    LookbackChoice(90L, "90 d"),
    LookbackChoice(365L, "1 y"),
)
private const val MAX_VISIBLE_WORKOUTS = 8
