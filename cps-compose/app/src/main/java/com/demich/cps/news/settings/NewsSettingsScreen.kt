package com.demich.cps.news.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.demich.cps.R
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.*
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch


@Composable
fun NewsSettingsScreen() {
    SettingsColumn {
        CodeforcesDefaultTabSettingsItem()
        CodeforcesLostSettingsItem()
        CodeforcesRuEnabledSettingsItem()
    }
}

@Composable
private fun CodeforcesDefaultTabSettingsItem() {
    val context = context
    SettingsEnumItem(
        item = context.settingsNews.codeforcesDefaultTab,
        title = "Default tab",
        options = listOf(
            CodeforcesTitle.MAIN,
            CodeforcesTitle.TOP,
            CodeforcesTitle.RECENT
        )
    )
}

@Composable
private fun CodeforcesLostSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()
    val enabled by rememberCollect { context.settingsNews.codeforcesLostEnabled.flow }
    SettingsItem {
        SettingsSwitchItemContent(
            checked = enabled,
            title = "Lost recent blog entries",
            description = stringResource(id = R.string.news_settings_cf_lost_description),
            onCheckedChange = { checked ->
                scope.launch { context.settingsNews.codeforcesLostEnabled(newValue = checked) }
            }
        )

    }
}

@Composable
private fun CodeforcesRuEnabledSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val locale by rememberCollect { context.settingsNews.codeforcesLocale.flow }

    SettingsSwitchItem(
        title = "Russian content",
        checked = locale == CodeforcesLocale.RU,
        onCheckedChange = { checked ->
            scope.launch {
                context.settingsNews.codeforcesLocale(
                    newValue = if (checked) CodeforcesLocale.RU else CodeforcesLocale.EN
                )
            }
        }
    )
}