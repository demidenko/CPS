package com.demich.cps.contests

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.*
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
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun ContestsSettingsScreen(navController: NavController) {
    val settings = with(context) { remember { settingsContests } }

    SettingsColumn {
        ContestPlatformsSettingsItem(
            item = settings.enabledPlatforms
        )
        ClistApiKeySettingsItem(item = settings.clistApiAccess)
    }
}

@Composable
private fun ClistApiKeySettingsItem(
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
            MonospacedText(text = "clist.by::api", modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp))
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
                            clistApiAccess(apiAccess)
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

@Composable
private fun ContestPlatformsSettingsItem(
    item: CPSDataStoreItem<Set<Contest.Platform>>
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = "Platforms"
    ) { enabledPlatforms ->
        val text = when {
            enabledPlatforms.isEmpty() -> "none selected"
            enabledPlatforms.size == Contest.getPlatforms().size -> "all selected"
            enabledPlatforms.size < 4 -> enabledPlatforms.joinToString(separator = ", ")
            else -> enabledPlatforms.toList().let { "${it[0]}, ${it[1]} and ${it.size - 2} more" }
        }
        Text(
            text = text,
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional
        )
    }

    if (showDialog) {
        ContestPlatformsDialog(onDismissRequest = { showDialog = false })
    }
}

@Composable
private fun ContestPlatformsDialog(onDismissRequest: () -> Unit) {
    val context = context
    val scope = rememberCoroutineScope()
    val settings = remember { context.settingsContests }
    val enabled by rememberCollect { settings.enabledPlatforms.flow }
    CPSDialog(onDismissRequest = onDismissRequest) {
        Contest.getPlatforms().forEach { platform ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContestPlatformIcon(
                    platform = platform,
                    size = 28.sp,
                    color = cpsColors.textColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                MonospacedText(text = platform.name, modifier = Modifier.weight(1f))
                CPSCheckBox(checked = platform in enabled) {
                     scope.launch {
                         context.settingsContests.enabledPlatforms(
                             newValue = if (it) enabled + platform else enabled - platform
                         )
                     }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
            TextButton(
                content = { Text(text = "Select all") },
                onClick = {
                    scope.launch {
                        context.settingsContests.enabledPlatforms(Contest.getPlatforms().toSet())
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )
            TextButton(
                content = { Text(text = "Close") },
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): CPSDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    val clistApiAccess = itemJsonable(name = "clist_api_access", defaultValue = CListApi.ApiAccess("", ""))

    val enabledPlatforms = itemEnumSet(name = "enabled_platforms", defaultValue = Contest.getPlatforms().toSet())
    val lastLoadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms", defaultValue = emptySet())
    val ignoredContests = itemJsonable(name = "ignored_contests_list", defaultValue = emptyList<Pair<Contest.Platform, String>>())
}
