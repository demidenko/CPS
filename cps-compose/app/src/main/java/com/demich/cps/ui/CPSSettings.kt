package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.CPSDataStoreItem
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun SettingsColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth(),
        content = content
    )
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp).fillMaxWidth())
    Box(
        modifier = modifier
            .background(cpsColors.backgroundAdditional)
            .fillMaxWidth()
            .padding(all = 10.dp)
    ) {
        content()
    }
}

@Composable
fun SwitchSettingsItem(
    item: CPSDataStoreItem<Boolean>,
    title: String,
    description: String = "",
) {
    val scope = rememberCoroutineScope()
    val checked by rememberCollect { item.flow }
    SwitchSettingsItem(
        checked = checked,
        title = title,
        description = description
    ) {
        scope.launch { item(it) }
    }
}

@Composable
private fun SwitchSettingsItem(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = cpsColors.textColorAdditional,
                        fontSize = 14.sp
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = 5.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cpsColors.colorAccent
                )
            )
        }
    }
}

@Composable
fun<T> SettingsItemWithInfo(
    modifier: Modifier = Modifier,
    item: CPSDataStoreItem<T>,
    title: String,
    infoContent: @Composable (T) -> Unit
) {
    val value by rememberCollect { item.flow }
    SettingsItem(modifier = modifier) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            infoContent(value)
        }
    }
}