package com.demich.cps.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.CPSIcons
import com.demich.cps.utils.clickableNoRipple

@Composable
fun SettingsContainerScope.Expandable(
    title: String,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Item(
        modifier = Modifier.clickableNoRipple(enabled = !expanded) { expanded = true }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = snap()),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                CollapseIcon(modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    text = title,
                    fontSize = CPSFontSize.settingsTitle,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { expanded = !expanded }
                )
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut(snap()) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "settings item content"
                ) { itemExpanded ->
                    if (itemExpanded) {
                        Box(
                            content = { expandedContent() },
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(top = 8.dp)
                        )
                    } else {
                        collapsedContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapseIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = CPSIcons.CollapseUp,
        contentDescription = null,
        modifier = modifier
    )
}