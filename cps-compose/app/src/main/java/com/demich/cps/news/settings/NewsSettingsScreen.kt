package com.demich.cps.news.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.*
import com.demich.cps.ui.dialogs.CPSDialogMultiSelectEnum
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.workers.CodeforcesNewsFollowWorker
import com.demich.cps.workers.CodeforcesNewsLostRecentWorker
import com.demich.cps.workers.NewsWorker
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun NewsSettingsScreen() {
    SettingsColumn {
        CodeforcesDefaultTabSettingsItem()
        CodeforcesFollowSettingsItem()
        CodeforcesLostSettingsItem()
        CodeforcesRuEnabledSettingsItem()
        NewsFeedsSettingsItem()
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
private fun CodeforcesFollowSettingsItem() {
    val context = context
    SettingsSwitchItemWithWork(
        item = context.settingsNews.codeforcesFollowEnabled,
        title = "Follow",
        description = stringResource(id = R.string.news_settings_cf_follow_description),
        workGetter = CodeforcesNewsFollowWorker::getWork
    )
}

@Composable
private fun CodeforcesLostSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()
    val settings = remember { context.settingsNews }
    val enabled by rememberCollect { settings.codeforcesLostEnabled.flow }
    SettingsItem {
        Column {
            SettingsSwitchItemContent(
                checked = enabled,
                title = "Lost recent blog entries",
                description = stringResource(id = R.string.news_settings_cf_lost_description),
                onCheckedChange = { checked ->
                    scope.launch {
                        settings.codeforcesLostEnabled(newValue = checked)
                        with(CodeforcesNewsLostRecentWorker.getWork(context)) {
                            if (checked) startImmediate() else stop()
                        }
                    }
                }
            )
            AnimatedVisibility(visible = enabled) {
                CodeforcesLostAuthorSettingsItem(item = settings.codeforcesLostMinRatingTag)
            }
        }
    }
}

@Composable
private fun CodeforcesLostAuthorSettingsItem(
    item: DataStoreItem<CodeforcesUtils.ColorTag>
) {
    val context = context
    val manager = remember { CodeforcesAccountManager(context) }
    val options = remember {
        listOf(
            CodeforcesUtils.ColorTag.BLACK to "Exists",
            CodeforcesUtils.ColorTag.GRAY to "Newbie",
            CodeforcesUtils.ColorTag.GREEN to "Pupil",
            CodeforcesUtils.ColorTag.CYAN to "Specialist",
            CodeforcesUtils.ColorTag.BLUE to "Expert",
            CodeforcesUtils.ColorTag.VIOLET to "Candidate Master",
            CodeforcesUtils.ColorTag.ORANGE to "Master",
            CodeforcesUtils.ColorTag.RED to "Grandmaster",
            CodeforcesUtils.ColorTag.LEGENDARY to "LGM"
        )
    }
    //TODO: restart worker on change?
    Box(modifier = Modifier.padding(top = 10.dp)) {
        SettingsEnumItemContent(
            item = item,
            title = "Author at least",
            options = options.map { it.first },
            optionToString = { tag ->
                manager.makeHandleSpan(
                    handle = options.first { it.first == tag }.second,
                    tag = tag
                )
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

@Composable
private fun NewsFeedsSettingsItem() {
    val context = context
    val enabledSettingsItem = remember { NewsSettingsDataStore(context).enabledNewsFeeds }

    val title = "News feeds"
    var showDialog by rememberSaveable { mutableStateOf(false) }

    SettingsItemWithInfo(
        item = enabledSettingsItem,
        title = title,
        modifier = Modifier.clickable { showDialog = true }
    ) { newsFeeds ->
        SettingsSubtitleOfEnabled(
            enabled = newsFeeds,
            name = { it.shortName }
        )
    }

    val scope = rememberCoroutineScope()
    if (showDialog) {
        CPSDialogMultiSelectEnum(
            title = title,
            options = NewsSettingsDataStore.NewsFeed.values().asIterable(),
            selectedOptions = remember { runBlocking { enabledSettingsItem() } },
            optionTitle = { Text(it.link) },
            onDismissRequest = { showDialog = false },
            onSaveSelected = { current ->
                scope.launch {
                    val newSelectedFeeds = current.toMutableSet().apply {
                        removeAll(enabledSettingsItem())
                        remove(NewsSettingsDataStore.NewsFeed.project_euler_problems)
                    }
                    enabledSettingsItem(newValue = current)
                    if (newSelectedFeeds.isNotEmpty()) {
                        NewsWorker.getWork(context).startImmediate()
                    }
                }
            }
        )
    }
}