package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun ClistApiKeySettingsItem(
    item: CPSDataStoreItem<CListApi.ApiAccess>
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = "Clist API access"
    ) { (login, key) ->
        if (login.isBlank()) {
            Text(
                text = "click to setup",
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional
            )
        } else {
            Text(
                text = buildAnnotatedString {
                    append(login)
                    if (key.isBlank()) withStyle(SpanStyle(color = cpsColors.errorColor)) {
                        append(" [api-key is empty]")
                    }
                },
                fontFamily = FontFamily.Monospace,
                color = cpsColors.textColor,
                fontSize = 13.sp
            )
        }
    }

    if (showDialog) {
        ClistApiDialog { showDialog = false }
    }
}

@Composable
private fun ClistApiDialog(onDismissRequest: () -> Unit) {
    val context = context
    val scope = rememberCoroutineScope()

    var apiAccess by rememberSaveable(stateSaver = jsonSaver()) {
        mutableStateOf(runBlocking { context.settingsContests.clistApiAccess() })
    }

    CPSDialog(onDismissRequest = onDismissRequest) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonospacedText(
                text = "clist.by::api", modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            CPSIconButton(icon = CPSIcons.Help) {
                context.openUrlInBrowser(CListApi.urls.apiHelp)
            }
        }

        ClistApiTextField(
            input = apiAccess.login,
            onChangeInput = { apiAccess = apiAccess.copy(login = it) },
            title = "login",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        ClistApiTextField(
            input = apiAccess.key,
            onChangeInput = { apiAccess = apiAccess.copy(key = it) },
            title = "api-key",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
            TextButton(
                onClick = {
                    scope.launch {
                        context.settingsContests.clistApiAccess(newValue = apiAccess)
                        onDismissRequest()
                    }
                },
                content = { Text("Save") }
            )
        }
    }
}

@Composable
private fun ClistApiTextField(
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
        label = { MonospacedText(text = title) },
        onValueChange = onChangeInput,
        isError = input.isBlank()
    )
}