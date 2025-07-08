package com.demich.cps.ui.settings

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.dialogs.CPSDialogSelect
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.launch

@Composable
fun <T: Enum<T>> SettingsContainerScope.SelectEnum(
    item: DataStoreItem<T>,
    title: String,
    description: String = "",
    optionToString: @Composable (T) -> AnnotatedString = { AnnotatedString(it.name) },
    options: List<T>
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