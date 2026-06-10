package com.demich.cps.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.dialogs.CPSDialogCancelAcceptButtons
import com.demich.cps.utils.rememberFirstValue
import com.demich.datastore_itemized.DataStoreValue
import kotlin.reflect.KProperty1

@Composable
context(scope: SettingsContainerScope)
internal fun <T> ApiAccessSettingsItem(
    item: DataStoreValue<T>,
    itemTitle: String,
    itemSubtitle: @Composable (context(SettingsContainerScope) (T) -> Unit),
    dialogTitle: String,
    fields: List<Pair<String, KProperty1<T & Any, String>>>,
    decode: (List<String>) -> T & Any,
    onSave: (T & Any) -> Unit,
    onHelp: (() -> Unit)? = null
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    SubtitledByValue(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = itemTitle,
        subtitle = itemSubtitle
    )

    if (showDialog) {
        val init = rememberFirstValue { item }

        ApiDialog(
            dialogTitle = dialogTitle,
            fields = fields.map { (title, prop) ->
                Field(title = title, initValue = init?.let { prop.get(it) } ?: "")
            },
            decode = decode,
            onSave = onSave,
            onDismissRequest = { showDialog = false },
            onHelp = onHelp
        )
    }
}

private class Field(
    val title: String,
    val initValue: String
)

@Composable
private fun <T> ApiDialog(
    dialogTitle: String,
    fields: List<Field>,
    decode: (List<String>) -> T,
    onSave: (T) -> Unit,
    onDismissRequest: () -> Unit,
    onHelp: (() -> Unit)?
) {
    CPSDialog(onDismissRequest = onDismissRequest) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dialogTitle,
                style = CPSDefaults.MonospaceTextStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            if (onHelp != null) {
                CPSIconButton(
                    icon = CPSIcons.Help,
                    onClick = onHelp
                )
            }
        }

        val strings = rememberSaveable {
            fields.map { it.initValue }.toMutableStateList()
        }

        fields.forEachIndexed { index, field ->
            ApiAccessFieldTextField(
                input = strings[index],
                onChangeInput = { strings[index] = it },
                title = field.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        CPSDialogCancelAcceptButtons(
            acceptTitle = "Save",
            onCancelClick = onDismissRequest,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            onSave(decode(strings))
            onDismissRequest()
        }
    }
}

@Composable
private fun ApiAccessFieldTextField(
    input: String,
    onChangeInput: (String) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    inputTextSize: TextUnit = 15.sp
) {
    TextField(
        modifier = modifier,
        value = input,
        singleLine = true,
        textStyle = TextStyle(fontSize = inputTextSize, fontFamily = FontFamily.Monospace),
        label = { Text(text = title, style = CPSDefaults.MonospaceTextStyle) },
        onValueChange = onChangeInput,
        isError = input.isBlank()
    )
}