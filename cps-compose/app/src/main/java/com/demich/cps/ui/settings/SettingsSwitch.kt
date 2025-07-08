package com.demich.cps.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSSwitch
import com.demich.cps.utils.collectItemAsState
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsContainerScope.SwitchItem(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    ItemWithTrailer(
        title = title,
        description = description
    ) {
        CPSSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
private inline fun SettingsContainerScope.SwitchItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    crossinline onCheckedChange: suspend CoroutineScope.(Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val checked by collectItemAsState { item }
    SwitchItem(
        checked = checked,
        title = title,
        description = description
    ) {
        scope.launch {
            item.setValue(it)
            onCheckedChange(it)
        }
    }
}

@Composable
fun SettingsContainerScope.SwitchItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = ""
) {
    SwitchItem(
        item = item,
        title = title,
        description = description,
        onCheckedChange = { }
    )
}