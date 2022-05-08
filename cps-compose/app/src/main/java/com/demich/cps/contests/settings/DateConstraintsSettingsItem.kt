package com.demich.cps.contests.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDialog
import com.demich.cps.ui.ExpandableSettingsItem
import com.demich.cps.ui.SettingsSubtitle
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun DateConstraintsSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val settings = remember { context.settingsContests }
    val dateConstraints by rememberCollect { settings.contestsDateConstraints.flow }

    fun saveConstraints(newConstraints: ContestDateConstraints) {
        scope.launch {
            settings.contestsDateConstraints(
                newValue = newConstraints
            )
        }
    }

    ExpandableSettingsItem(
        title = "Date constraints",
        collapsedContent = { SettingsSubtitle(makeInfoString(dateConstraints)) },
        expandedContent = {
            Column {
                DurationRow(
                    title = "Max duration = ",
                    duration = dateConstraints.maxDuration,
                    onDurationChange = { saveConstraints(dateConstraints.copy(maxDuration = it)) }
                )
                DurationRow(
                    title = "Max start - now = ",
                    duration = dateConstraints.nowToStartTimeMaxDuration,
                    onDurationChange = { saveConstraints(dateConstraints.copy(nowToStartTimeMaxDuration = it)) }
                )
                DurationRow(
                    title = "Max now - end = ",
                    duration = dateConstraints.endTimeToNowMaxDuration,
                    onDurationChange = { saveConstraints(dateConstraints.copy(endTimeToNowMaxDuration = it)) }
                )
            }
        }
    )
}

private fun makeInfoString(dateConstraints: ContestDateConstraints): String =
    with(dateConstraints) {
        "duration ≤ $maxDuration; start ≤ now + $nowToStartTimeMaxDuration; now - $endTimeToNowMaxDuration ≤ end"
    }

@Composable
private fun DurationRow(
    modifier: Modifier = Modifier,
    title: String,
    duration: Duration,
    onDurationChange: (Duration) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        TextButton(
            onClick = { showDialog = true },
            content = { Text(text = "$duration", fontWeight = FontWeight.Bold) }
        )
    }

    if(showDialog) {
        DurationPickerDialog(
            title = title,
            initDuration = duration,
            onDismissRequest = { showDialog = false },
            onDurationSelect = onDurationChange
        )
    }
}


@Composable
private fun DurationPickerDialog(
    title: String,
    initDuration: Duration,
    onDismissRequest: () -> Unit,
    onDurationSelect: (Duration) -> Unit,
) {
    var inDays by remember {
        initDuration.toComponents { days, hours, _, _, _ ->
            mutableStateOf(hours == 0)
        }
    }
    var input by remember {
        val number = if (inDays) initDuration.inWholeDays
        else initDuration.inWholeHours
        mutableStateOf(number.toString())
    }

    val duration: Duration? by remember {
        derivedStateOf {
            input.toIntOrNull()?.let { number ->
                if (inDays) number.days else number.hours
            }
        }
    }

    CPSDialog(onDismissRequest = onDismissRequest) {
        Text(title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = input,
                onValueChange = { str ->
                    input = str.filter { it.isDigit() }
                },
                textStyle = TextStyle(
                    color = cpsColors.textColor,
                    fontSize = 18.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            Column {
                RadioButtonTitled(title = "hours", selected = !inDays) {
                    inDays = false
                }
                RadioButtonTitled(title = "days", selected = inDays) {
                    inDays = true
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                content = { Text("Cancel") },
                onClick = onDismissRequest
            )
            TextButton(
                enabled = duration != null,
                content = { Text("Save") },
                onClick = {
                    onDurationSelect(duration!!)
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun RadioButtonTitled(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title)
    }
}