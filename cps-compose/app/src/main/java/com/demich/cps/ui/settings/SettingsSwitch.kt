package com.demich.cps.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSSwitch
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.workers.CPSPeriodicWorkProvider
import com.demich.cps.workers.ProfilesWorker
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
context(scope: SettingsContainerScope)
fun Switch(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    scope.ItemWithTrailer(
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
context(scope: SettingsContainerScope)
private inline fun SwitchByItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    crossinline onCheckedChange: suspend CoroutineScope.(Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val checked by collectItemAsState { item }
    Switch(
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
context(scope: SettingsContainerScope)
fun SwitchByItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = ""
) {
    SwitchByItem(
        item = item,
        title = title,
        description = description,
        onCheckedChange = { }
    )
}

@Composable
context(scope: SettingsContainerScope)
fun SwitchByWork(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    workProvider: CPSPeriodicWorkProvider,
    stopWorkOnUnchecked: Boolean = true
) {
    val context = context
    SwitchByItem(
        item = item,
        title = title,
        description = description
    ) { checked ->
        with(workProvider.getWork(context)) {
            if (checked) startImmediate()
            else if (stopWorkOnUnchecked) stop()
        }
    }
}

@Composable
context(scope: SettingsContainerScope)
fun SwitchByProfilesWork(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = ""
) {
    SwitchByWork(
        item = item,
        title = title,
        description = description,
        workProvider = ProfilesWorker,
        stopWorkOnUnchecked = false
    )
}