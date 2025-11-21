package com.demich.cps.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.theme.cpsColors

@Composable
context(scope: SettingsContainerScope)
fun Item(
    modifier: Modifier = Modifier,
    content: @Composable context(SettingsContainerScope) () -> Unit
) {
    scope.append(modifier = modifier) {
        ColumnSpaced(
            // TODO: bad glitch with animated content
            space = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            context(SettingsItemScopeInstance) {
                content()
            }
        }
    }
}

@Composable
context(scope: SettingsContainerScope)
fun Title(
    modifier: Modifier = Modifier,
    title: String
) {
    Text(
        text = title,
        fontSize = CPSFontSize.settingsTitle,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
context(scope: SettingsContainerScope)
fun ItemWithTrailer(
    title: String,
    description: String = "",
    trailer: @Composable () -> Unit
) {
    Item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            ColumnSpaced(
                space = 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Title(title = title)
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

@Composable
inline fun ColumnSpaced(
    modifier: Modifier = Modifier,
    space: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = space),
        modifier = modifier,
        content = content
    )
}

private object SettingsItemScopeInstance: SettingsContainerScope {
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