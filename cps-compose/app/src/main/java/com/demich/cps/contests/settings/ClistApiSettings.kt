package com.demich.cps.contests.settings

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.demich.cps.platforms.api.clist.ClistApiAccess
import com.demich.cps.platforms.api.clist.ClistUrls
import com.demich.cps.ui.settings.ApiAccessSettingsItem
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.value
import kotlinx.coroutines.launch

@Composable
context(scope: SettingsContainerScope)
internal fun ClistApiAccessSettingsItem(
    settings: ContestsSettingsDataStore
) {
    val context = context
    val scope = rememberCoroutineScope()

    ApiAccessSettingsItem(
        item = settings.clistApiAccess,
        itemTitle = "Clist API access",
        itemSubtitle = {
            if (it == null) {
                Text(text = "undefined")
            } else {
                Text(
                    text = buildAnnotatedString {
                        append(it.login)
                        if (it.key.isBlank()) append(" [api-key is empty]", color = cpsColors.error)
                    },
                    fontFamily = FontFamily.Monospace,
                    color = cpsColors.content,
                    fontSize = 13.sp
                )
            }
        },
        dialogTitle = "clist.by::api",
        fields = listOf(
            "login" to ClistApiAccess::login,
            "api-key" to ClistApiAccess::key,
        ),
        decode = { list ->
            ClistApiAccess(login = list[0], key = list[1])
        },
        onSave = {
            scope.launch {
                settings.edit {
                    clistApiLogin.value = it.login
                    clistApiKey.value = it.key
                }
            }
        },
        onHelp = {
            context.openUrlInBrowser(ClistUrls.apiHelp)
        }
    )
}
