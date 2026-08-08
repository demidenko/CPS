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
import com.demich.cps.utils.FetchState
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
    onHelp: (() -> Unit)? = null,
    checkRequest: (suspend (T & Any) -> Unit)? = null
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
            title = dialogTitle,
            fields = fields.map { (title, prop) ->
                Field(title = title, initValue = init?.let { prop.get(it) } ?: "")
            },
            decode = decode,
            onSave = onSave,
            onDismissRequest = { showDialog = false },
            onHelp = onHelp,
            checkRequest = checkRequest
        )
    }
}

private class Field(
    val title: String,
    val initValue: String
)

@Composable
private fun <T: Any> ApiDialog(
    title: String,
    fields: List<Field>,
    decode: (List<String>) -> T,
    onSave: (T) -> Unit,
    onDismissRequest: () -> Unit,
    onHelp: (() -> Unit)?,
    checkRequest: (suspend (T) -> Unit)? = null
) {
    CPSDialog(onDismissRequest = onDismissRequest) {
        ApiAccessEditorHeader(
            modifier = Modifier.fillMaxWidth(),
            title = title,
            onHelp = onHelp
        )

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

        ApiAccessControls(
            modifier = Modifier.padding(top = 8.dp),
            onCancelRequest = onDismissRequest,
            onSaveResuest = {
                onSave(decode(strings))
                onDismissRequest()
            },
            onCheckRequest = {
                TODO()
            },
            checkFetchState = { TODO() }
        )
    }
}

@Composable
private fun ApiAccessControls(
    modifier: Modifier = Modifier,
    onSaveResuest: () -> Unit,
    onCancelRequest: () -> Unit,
    onCheckRequest: () -> Unit,
    checkFetchState: () -> FetchState<*>?
) {
    CPSDialogCancelAcceptButtons(
        modifier = modifier,
        acceptTitle = "Save",
        onCancelClick = onCancelRequest,
        onAcceptClick = onSaveResuest,
    )
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

@Composable
private fun ApiAccessEditorHeader(
    modifier: Modifier = Modifier,
    title: String,
    onHelp: (() -> Unit)?
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
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
}