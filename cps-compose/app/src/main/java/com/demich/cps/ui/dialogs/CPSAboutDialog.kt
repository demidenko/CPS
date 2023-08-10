package com.demich.cps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.BuildConfig
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.context
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun CPSAboutDialog(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    val context = context
    val devModeEnabled by rememberCollect { context.settingsUI.devModeEnabled.flow }
    var showDevModeLine by remember { mutableStateOf(devModeEnabled) }

    val onClick = remember {
        patternClickListener("._.._...") {
            showDevModeLine = true
            scope.launch { context.settingsUI.devModeEnabled(true) }
        }
    }

    CPSDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(enabled = !showDevModeLine, onClick = onClick)
    ) {
        MonospacedText("   Competitive")
        MonospacedText("   Programming")
        MonospacedText("&& Solving")
        MonospacedText("{")
        MonospacedText("   version = ${BuildConfig.VERSION_NAME}" + if (devModeEnabled) " (${BuildConfig.VERSION_CODE})" else "")
        if (showDevModeLine) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonospacedText("   dev_mode = $devModeEnabled", Modifier.weight(1f))
                CPSCheckBox(checked = devModeEnabled) { checked ->
                    scope.launch { context.settingsUI.devModeEnabled(checked) }
                }
            }
        }
        MonospacedText("}")
    }
}

private fun patternClickListener(
    pattern: String,
    onMatch: () -> Unit
): () -> Unit {
    fun badPattern() { throw Exception("Bad pattern: [$pattern]") }

    if (pattern[0] != '.') badPattern()

    var pushes = 1
    val shorts = mutableListOf<Int>()
    val longs = mutableListOf<Int>()
    for (i in 1 until pattern.length) {
        when (pattern[i]) {
            '.' -> {
                pushes++
                if (pattern[i-1] == '.') shorts.add(pushes-2)
                else longs.add(pushes-2)
            }
            '_' -> if (pattern[i-1] != '.') badPattern()
            else -> badPattern()
        }
    }

    val n = pushes
    val shortIndices = shorts.toIntArray()
    val longIndices = longs.toIntArray()

    val timeClicks = Array(n) { Instant.DISTANT_PAST }
    var clicks = 0

    return {
        getCurrentTime().let { cur ->
            if(clicks > 0 && cur - timeClicks[(clicks-1)%n] > 1.seconds) clicks = 0
            timeClicks[clicks%n] = cur
        }
        ++clicks
        if (clicks >= n) {
            val t = (n-2 downTo 0).map { i ->
                timeClicks[(clicks-1-i)%n] - timeClicks[(clicks-2-i)%n]
            }

            val x: Duration = longIndices.minOf { t[it] }
            val y: Duration = shortIndices.maxOf { t[it] }

            if(x > y) {
                clicks = 0
                onMatch()
            }
        }
    }
}