package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.Contest
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.CPSDialog
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.SettingsItemWithInfo
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.CPSDataStoreItem
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun ContestPlatformsSettingsItem(
    item: CPSDataStoreItem<Set<Contest.Platform>>
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = "Platforms"
    ) { enabledPlatforms ->
        val platforms = enabledPlatforms - Contest.Platform.unknown
        val text = when {
            platforms.isEmpty() -> "none selected"
            platforms.size == Contest.platformsExceptUnknown.size -> "all selected"
            platforms.size < 4 -> platforms.joinToString(separator = ", ")
            else -> platforms.toList().let { "${it[0]}, ${it[1]} and ${it.size - 2} more" }
        }
        Text(
            text = text,
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional
        )
    }

    if (showDialog) {
        ContestPlatformsDialog(onDismissRequest = { showDialog = false })
    }
}

@Composable
private fun ContestPlatformsDialog(onDismissRequest: () -> Unit) {
    val context = context
    val scope = rememberCoroutineScope()
    val settings = remember { context.settingsContests }
    val enabled by rememberCollect { settings.enabledPlatforms.flow }
    CPSDialog(onDismissRequest = onDismissRequest) {
        Contest.platformsExceptUnknown.forEach { platform ->
            PlatformCheckBox(
                platform = platform,
                isChecked = platform in enabled
            ) { checked ->
                scope.launch {
                    context.settingsContests.enabledPlatforms(
                        newValue = if (checked) enabled + platform else enabled - platform
                    )
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp)) {
            TextButton(
                content = { Text(text = "Select all") },
                onClick = {
                    scope.launch {
                        context.settingsContests.enabledPlatforms(Contest.platforms.toSet())
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )
            TextButton(
                content = { Text(text = "Close") },
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun PlatformCheckBox(
    platform: Contest.Platform,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContestPlatformIcon(
            platform = platform,
            size = 28.sp,
            color = cpsColors.textColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        MonospacedText(text = platform.name, modifier = Modifier.weight(1f))
        CPSCheckBox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}