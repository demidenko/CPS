package com.demich.cps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.BuildConfig
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun CPSAboutDialog(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    val context = context
    val devModeEnabled by collectItemAsState { context.settingsUI.devModeEnabled }
    var showDevModeLine by remember { mutableStateOf(devModeEnabled) }

    val onClick = remember {
        patternClickListener(pattern = "._.._...", getCurrentTime = ::getCurrentTime) {
            showDevModeLine = true
            scope.launch { context.settingsUI.devModeEnabled.setValue(true) }
        }
    }

    CPSDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(enabled = !showDevModeLine, onClick = onClick)
    ) {
        DialogContent(
            devModeEnabled = devModeEnabled,
            showDevModeLine = showDevModeLine,
            onDevModeChange = { checked ->
                scope.launch { context.settingsUI.devModeEnabled.setValue(checked) }
            }
        )
    }
}

@Composable
private fun DialogContent(
    devModeEnabled: Boolean,
    showDevModeLine: Boolean,
    onDevModeChange: (Boolean) -> Unit
) {
    ProvideTextStyle(value = CPSDefaults.MonospaceTextStyle) {
        Text("   Competitive")
        Text("   Programming")
        Text("&& Solving")
        Text("{")
        Text("   version = ${cpsVersion(devModeEnabled)}")
        if (showDevModeLine) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("   dev_mode = $devModeEnabled", Modifier.weight(1f))
                CPSCheckBox(checked = devModeEnabled, onCheckedChange = onDevModeChange)
            }
        }
        Text("}")
    }
}

private fun cpsVersion(devModeEnabled: Boolean): String =
    BuildConfig.VERSION_NAME + if (devModeEnabled) " (${BuildConfig.VERSION_CODE})" else ""

private fun patternClickListener(
    pattern: String,
    tap: Char = '.',
    pause: Char = '_',
    getCurrentTime: () -> Instant,
    onMatch: () -> Unit
): () -> Unit {
    require(tap != pause)
    require(pattern.all { it == tap || it == pause })
    require(pattern.isNotEmpty() && pattern[0] == tap)
    require(pattern.count { it == tap } >= 2)
    require(pattern.count { it == pause } >= 1)
    require("$pause$pause" !in pattern)

    var pushes = 1
    val shorts = mutableListOf<Int>()
    val longs = mutableListOf<Int>()
    for (i in 1 until pattern.length) {
        if (pattern[i] == tap) {
            pushes++
            if (pattern[i-1] == tap) shorts.add(pushes-2)
            else longs.add(pushes-2)
        }
    }

    val n = pushes
    val shortIndices = shorts.toIntArray()
    val longIndices = longs.toIntArray()

    val timeClicks = Array(n) { Instant.DISTANT_PAST }
    var clicks = 0

    return {
        getCurrentTime().let { cur ->
            if (clicks > 0 && cur - timeClicks[(clicks-1)%n] > 1.seconds) clicks = 0
            timeClicks[clicks%n] = cur
        }
        ++clicks
        if (clicks >= n) {
            val t = (n-2 downTo 0).map { i ->
                timeClicks[(clicks-1-i)%n] - timeClicks[(clicks-2-i)%n]
            }

            val x: Duration = longIndices.minOf { t[it] }
            val y: Duration = shortIndices.maxOf { t[it] }

            if (x > y) {
                clicks = 0
                onMatch()
            }
        }
    }
}