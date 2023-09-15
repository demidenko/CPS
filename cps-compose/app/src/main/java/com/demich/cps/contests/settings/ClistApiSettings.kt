package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.SettingsItemWithInfo
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.dialogs.CPSDialogCancelAcceptButtons
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.ui.CPSDefaults
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
internal fun ClistApiAccessSettingsItem() {
    val context = context
    val settings = remember { context.settingsContests }

    var showDialog by rememberSaveable { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = settings.clistApiAccess,
        title = "Clist API access"
    ) { (login, key) ->
        if (login.isBlank()) {
            Text(
                text = "click to setup",
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
        } else {
            Text(
                text = buildAnnotatedString {
                    append(login)
                    if (key.isBlank()) append(" [api-key is empty]", color = cpsColors.error)
                },
                fontFamily = FontFamily.Monospace,
                color = cpsColors.content,
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

    var apiAccess by rememberSaveable(stateSaver = jsonCPS.saver()) {
        mutableStateOf(runBlocking { context.settingsContests.clistApiAccess() })
    }

    CPSDialog(onDismissRequest = onDismissRequest) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "clist.by::api",
                style = CPSDefaults.MonospaceTextStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            CPSIconButton(icon = CPSIcons.Help) {
                context.openUrlInBrowser(ClistApi.urls.apiHelp)
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

        CPSDialogCancelAcceptButtons(
            acceptTitle = "Save",
            onCancelClick = onDismissRequest,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            scope.launch {
                context.settingsContests.clistApiAccess(newValue = apiAccess)
                onDismissRequest()
            }
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
        label = { Text(text = title, style = CPSDefaults.MonospaceTextStyle) },
        onValueChange = onChangeInput,
        isError = input.isBlank()
    )
}