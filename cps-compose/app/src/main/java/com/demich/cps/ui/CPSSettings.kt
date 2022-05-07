package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
            .padding(all = 10.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(cpsColors.backgroundAdditional, RoundedCornerShape(4.dp))
            .fillMaxWidth()
            .padding(all = 10.dp),
        contentAlignment = Alignment.CenterStart
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
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
    SettingsItem(modifier = modifier) {
        val value by rememberCollect { item.flow }
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            infoContent(value)
        }
    }
}