package com.demich.cps.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.theme.cpsColors

@Composable
fun SettingsContainerScope.Item(
    modifier: Modifier = Modifier,
    content: @Composable SettingsContainerScope.() -> Unit
) {
    append(modifier = modifier) {
        Column(
            // TODO: bad glitch with animated content
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsItemScopeInstance.content()
        }
    }
}

@Composable
fun SettingsContainerScope.ItemWithTrailer(
    title: String,
    description: String = "",
    trailer: @Composable () -> Unit
) {
    Item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = CPSFontSize.settingsTitle,
                    fontWeight = FontWeight.SemiBold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = cpsColors.contentAdditional,
                        fontSize = CPSFontSize.settingsDescription
                    )
                }
            }
            trailer()
        }
    }
}

object SettingsItemScopeInstance: SettingsContainerScope {
    @Composable
    override fun append(
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}