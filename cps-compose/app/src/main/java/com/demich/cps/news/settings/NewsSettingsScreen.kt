package com.demich.cps.news.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.ui.SettingsEnumItem
import com.demich.cps.ui.SettingsSwitchItem
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch


@Composable
fun NewsSettingsScreen() {
    SettingsColumn {
        CodeforcesDefaultTab()
        CodeforcesRuEnabledSettingsItem()
    }
}

@Composable
private fun CodeforcesDefaultTab() {
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