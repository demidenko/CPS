package com.demich.cps.news.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.*
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.codeforces.CodeforcesUtils
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
    val manager = remember { CodeforcesAccountManager(context) }
    val scope = rememberCoroutineScope()
    val settings = remember { context.settingsNews }
    val enabled by rememberCollect { settings.codeforcesLostEnabled.flow }
    SettingsItem {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsSwitchItemContent(
                checked = enabled,
                title = "Lost recent blog entries",
                description = stringResource(id = R.string.news_settings_cf_lost_description),
                onCheckedChange = { checked ->
                    scope.launch { settings.codeforcesLostEnabled(newValue = checked) }
                }
            )
            AnimatedVisibility(visible = enabled) {
                SettingsEnumItemContent(
                    item = settings.codeforcesLostMinRatingTag,
                    title = "Author at least",
                    options = listOf(
                        CodeforcesUtils.ColorTag.BLACK,
                        CodeforcesUtils.ColorTag.GRAY,
                        CodeforcesUtils.ColorTag.GREEN,
                        CodeforcesUtils.ColorTag.CYAN,
                        CodeforcesUtils.ColorTag.BLUE,
                        CodeforcesUtils.ColorTag.VIOLET,
                        CodeforcesUtils.ColorTag.ORANGE,
                        CodeforcesUtils.ColorTag.RED,
                        CodeforcesUtils.ColorTag.LEGENDARY
                    ),
                    optionToString = {
                        val name: String = when (it) {
                            CodeforcesUtils.ColorTag.BLACK -> "Exists"
                            CodeforcesUtils.ColorTag.GRAY -> "Newbie"
                            CodeforcesUtils.ColorTag.GREEN -> "Pupil"
                            CodeforcesUtils.ColorTag.CYAN -> "Specialist"
                            CodeforcesUtils.ColorTag.BLUE -> "Expert"
                            CodeforcesUtils.ColorTag.VIOLET -> "Candidate Master"
                            CodeforcesUtils.ColorTag.ORANGE -> "Master"
                            CodeforcesUtils.ColorTag.RED -> "Grandmaster"
                            CodeforcesUtils.ColorTag.LEGENDARY -> "LGM"
                            else -> ""
                        }
                        manager.makeHandleSpan(handle = name, tag = it)
                    }
                )
            }
        }
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