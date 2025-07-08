package com.demich.cps.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.dialogs.CPSDialogMultiSelectEnum
import com.demich.cps.ui.dialogs.CPSDialogSelect
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.launch

@Composable
fun <T: Enum<T>> SettingsContainerScope.SelectEnum(
    title: String,
    description: String = "",
    item: DataStoreItem<T>,
    options: List<T>,
    optionToString: @Composable (T) -> AnnotatedString = { AnnotatedString(it.name) }
) {
    val scope = rememberCoroutineScope()
    val selectedOption by collectItemAsState { item }

    var showChangeDialog by rememberSaveable { mutableStateOf(false) }

    ItemWithTrailer(
        title = title,
        description = description
    ) {
        TextButton(onClick = { showChangeDialog = true }) {
            Text(
                text = optionToString(selectedOption),
                fontSize = CPSFontSize.settingsTitle,
                color = cpsColors.accent
            )
        }
    }

    if (showChangeDialog) {
        CPSDialogSelect(
            title = title,
            options = options,
            selectedOption = selectedOption,
            optionTitle = { Text(text = optionToString(it)) },
            onDismissRequest = { showChangeDialog = false },
            onSelectOption = {
                scope.launch { item.setValue(it) }
            }
        )
    }
}

@Composable
fun <T: Enum<T>> SettingsContainerScope.MultiSelectEnum(
    title: String,
    item: DataStoreItem<Set<T>>,
    options: List<T>,
    optionName: (T) -> String = { it.name },
    onNewSelected: suspend (Set<T>) -> Unit,
    optionContent: @Composable (T) -> Unit = { Text(text = optionName(it)) }
) {
    val scope = rememberCoroutineScope()
    val selectedOptions by collectItemAsState { item }

    var showChangeDialog by rememberSaveable { mutableStateOf(false) }

    SubtitledByValue(
        item = item,
        title = title,
        modifier = Modifier.clickable { showChangeDialog = true }
    ) { newsFeeds ->
        Subtitle(
            selected = selectedOptions,
            name = optionName
        )
    }

    if (showChangeDialog) {
        CPSDialogMultiSelectEnum(
            title = title,
            options = options,
            selectedOptions = selectedOptions,
            optionContent = optionContent,
            onDismissRequest = { showChangeDialog = false },
            onSaveSelected = {
                scope.launch {
                    item.setValue(it)
                    onNewSelected(it - selectedOptions)
                }
            }
        )
    }
}