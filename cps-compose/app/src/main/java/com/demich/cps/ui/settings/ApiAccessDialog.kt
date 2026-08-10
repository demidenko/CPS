package com.demich.cps.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.platforms.clients.niceMessage
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.dialogs.CPSDialogCancelAcceptButtons
import com.demich.cps.utils.FetchResult
import com.demich.cps.utils.FetchResult.Success
import com.demich.cps.utils.FetchState
import com.demich.cps.utils.fetchFlowOf
import com.demich.cps.utils.rememberFirstValue
import com.demich.datastore_itemized.DataStoreValue
import com.sebaslogen.resaca.viewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    checkBlock: suspend (T & Any) -> Unit // TODO nullable lambda
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
            checkBlock = checkBlock
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
    checkBlock: suspend (T) -> Unit
) {
    CPSDialog(
        onDismissRequest = onDismissRequest,
        dismissOnClickOutside = false
    ) {
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
            currentValue = { decode(strings) },
            checkBlock = checkBlock,
            onSave = onSave,
            onCancelRequest = onDismissRequest
        )
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

@Composable
private fun <T: Any> ApiAccessControls(
    modifier: Modifier = Modifier,
    currentValue: () -> T,
    checkBlock: suspend (T) -> Unit,
    onSave: (T) -> Unit,
    onCancelRequest: () -> Unit
) {
    val viewModel = viewModelScoped { ApiAccessCheckViewModel() }
    val fetchState = viewModel.flowOfFetchState.collectAsState()

    ApiAccessControls(
        modifier = modifier,
        onCancelRequest = onCancelRequest,
        onSaveResuest = {
            onSave(currentValue())
            onCancelRequest()
        },
        onCheckRequest = {
            val access = currentValue()
            viewModel.launchCheck { checkBlock(access) }
        },
        checkFetchState = fetchState::value
    )
}

@Composable
private fun ApiAccessControls(
    modifier: Modifier = Modifier,
    onSaveResuest: () -> Unit,
    onCancelRequest: () -> Unit,
    onCheckRequest: () -> Unit,
    checkFetchState: () -> FetchState<*>?
) {
    val fetchState = checkFetchState()
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ApiAccessCheckButton(
                modifier = Modifier.align(Alignment.TopStart),
                onCheckRequest = onCheckRequest,
                fetchState = fetchState
            )
            CPSDialogCancelAcceptButtons(
                acceptTitle = "Save",
                onCancelClick = onCancelRequest,
                onAcceptClick = onSaveResuest,
            )
        }
        if (fetchState is FetchResult) {
            ApiAccessCheckResultMessage(
                result = fetchState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ApiAccessCheckButton(
    modifier: Modifier = Modifier,
    onCheckRequest: () -> Unit,
    fetchState: FetchState<*>?
) {
    TextButton(
        modifier = modifier,
        enabled = fetchState != Loading,
        onClick = onCheckRequest
    ) {
        val text = when (fetchState) {
            Loading -> "checking..."
            else -> "check"
        }
        Text(text = text)
    }
}

@Composable
private fun ApiAccessCheckResultMessage(
    modifier: Modifier = Modifier,
    result: FetchResult<*>
) {
    val text = when (result) {
        is Failure -> {
            val message = result.exception.niceMessage
            if (message == null) "check failed"
            else "check failed: $message"
        }
        is Success -> "successful check"
    }
    Text(
        text = text,
        modifier = modifier,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private class ApiAccessCheckViewModel: ViewModel() {

    val flowOfFetchState: StateFlow<FetchState<Unit>?>
        field = MutableStateFlow(null)

    fun launchCheck(
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            fetchFlowOf(block)
                .collect {
                    flowOfFetchState.value = it
                }
        }
    }
}