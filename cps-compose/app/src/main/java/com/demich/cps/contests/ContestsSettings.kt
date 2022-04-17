package com.demich.cps.contests

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.CListAPI
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun ContestsSettingsScreen(navController: NavController) {
    val settings = with(context) { remember { settingsContests } }

    var showCListApiDialog by rememberSaveable { mutableStateOf(false) }

    SettingsColumn {
        SettingsItemWithInfo(
            modifier = Modifier.clickable { showCListApiDialog = true },
            item = settings.clistApiLoginAndKey,
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
    }

    if (showCListApiDialog) {
        CListAPIDialog { showCListApiDialog = false }
    }
}

@Composable
private fun CListAPIDialog(
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    var apiLogin by rememberSaveable {
        mutableStateOf(
            runBlocking { context.settingsContests.clistApiLoginAndKey().first }
        )
    }

    var apiKey by rememberSaveable {
        mutableStateOf(
            runBlocking { context.settingsContests.clistApiLoginAndKey().second }
        )
    }

    CPSDialog(onDismissRequest = onDismissRequest) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonospacedText(text = "clist.by::api", modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp))
            CPSIconButton(icon = Icons.Default.HelpOutline) {
                context.openUrlInBrowser(CListAPI.URLFactory.apiHelp)
            }
        }

        ClistTextField(
            input = apiLogin,
            onChangeInput = { apiLogin = it },
            title = "login",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        ClistTextField(
            input = apiKey,
            onChangeInput = { apiKey = it },
            title = "api-key",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
            TextButton(
                onClick = {
                    scope.launch {
                        with(context.settingsContests) {
                            clistApiLoginAndKey(newValue = apiLogin to apiKey)
                        }
                        onDismissRequest()
                    }
                },
                content = { Text("Save") }
            )
        }
    }
}

@Composable
private fun ClistTextField(
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



val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): CPSDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    val clistApiLoginAndKey = itemJsonable(name = "clist_api_login_key", defaultValue = Pair("", ""))
}
