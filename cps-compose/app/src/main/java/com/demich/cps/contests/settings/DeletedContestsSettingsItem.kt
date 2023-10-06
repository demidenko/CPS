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
import com.demich.cps.contests.ContestsInfoDataStore
import com.demich.cps.ui.SettingsItemWithInfo
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.utils.context
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.launch

@Composable
internal fun DeletedContestsSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val settings = remember { ContestsInfoDataStore(context) }

    var showDialog by remember { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = settings.ignoredContests,
        title = "Ignored contests"
    ) {
        Text(text = it.size.toString())
    }

    if (showDialog) {
        CPSYesNoDialog(
            title = { Text("Reset ignored?") },
            onDismissRequest = { showDialog = false },
            onConfirmRequest = {
                scope.launch {
                    settings.ignoredContests.edit { clear() }
                }
            }
        )
    }
}