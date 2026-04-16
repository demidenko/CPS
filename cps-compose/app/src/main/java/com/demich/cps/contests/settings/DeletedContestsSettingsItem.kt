package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.demich.cps.contests.ContestCompositeId
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.settings.SubtitledByValue
import com.demich.cps.utils.TimedCollection
import com.demich.cps.utils.emptyTimedCollection
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.setValueIn

@Composable
context(scope: SettingsContainerScope)
internal fun DeletedContestsSettingsItem(
    item: DataStoreItem<TimedCollection<ContestCompositeId>>
) {
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    SubtitledByValue(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = "Ignored contests"
    ) {
        Text(text = it.size.toString())
    }

    if (showDialog) {
        CPSYesNoDialog(
            title = { Text("Reset ignored?") },
            onDismissRequest = { showDialog = false },
            onConfirmRequest = {
                item.setValueIn(scope, emptyTimedCollection())
            }
        )
    }
}