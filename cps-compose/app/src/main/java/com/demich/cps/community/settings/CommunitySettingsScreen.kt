package com.demich.cps.community.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.R
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.community.codeforces.CodeforcesTitle
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.SettingsColumn
import com.demich.cps.ui.SettingsEnumItem
import com.demich.cps.ui.SettingsEnumItemContent
import com.demich.cps.ui.SettingsItem
import com.demich.cps.ui.SettingsItemWithInfo
import com.demich.cps.ui.SettingsSectionHeader
import com.demich.cps.ui.SettingsSubtitleOfEnabled
import com.demich.cps.ui.SettingsSwitchItem
import com.demich.cps.ui.SettingsSwitchItemContent
import com.demich.cps.ui.SettingsSwitchItemWithWork
import com.demich.cps.ui.dialogs.CPSDialogMultiSelectEnum
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.workers.CodeforcesCommunityFollowWorker
import com.demich.cps.workers.CodeforcesCommunityLostRecentWorker
import com.demich.cps.workers.NewsWorker
import com.demich.cps.workers.ProjectEulerRecentProblemsWorker
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Composable
fun CommunitySettingsScreen() {
    val context = context
    val devEnabled by collectItemAsState { context.settingsUI.devModeEnabled }

    val requiredPermissions by remember {
        flowOfNotificationPermissionsRequired(context)
    }.collectAsState(initial = false)

    SettingsColumn(
        requiredNotificationsPermission = requiredPermissions,
        modifier = Modifier.fillMaxHeight()
    ) {
        SettingsSectionHeader(
            title = "codeforces",
            painter = platformIconPainter(platform = Contest.Platform.codeforces)
        ) {
            CodeforcesDefaultTabSettingsItem()
            CodeforcesFollowSettingsItem()
            CodeforcesLostSettingsItem()
            CodeforcesRuEnabledSettingsItem()
        }

        SettingsSectionHeader(
            title = "news feeds",
            painter = rememberVectorPainter(image = CPSIcons.NewsFeeds)
        ) {
            NewsFeedsSettingsItem()
        }

        if (devEnabled) {
            SettingsSectionHeader(
                title = "dev",
                painter = rememberVectorPainter(image = CPSIcons.Development)
            ) {
                RenderAllTabs()
            }
        }
    }
}

private fun flowOfNotificationPermissionsRequired(context: Context): Flow<Boolean> =
    context.settingsCommunity.flowOf { prefs ->
        prefs[codeforcesFollowEnabled] || prefs[enabledNewsFeeds].isNotEmpty()
    }

@Composable
private fun CodeforcesDefaultTabSettingsItem() {
    val context = context
    SettingsEnumItem(
        item = context.settingsCommunity.codeforcesDefaultTab,
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
        item = context.settingsCommunity.codeforcesFollowEnabled,
        title = "Follow",
        description = stringResource(id = R.string.community_settings_cf_follow_description),
        workGetter = CodeforcesCommunityFollowWorker::getWork
    )
}

@Composable
private fun CodeforcesLostSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()
    val settings = remember { context.settingsCommunity }
    val enabled by collectItemAsState { settings.codeforcesLostEnabled }
    SettingsItem {
        Column {
            SettingsSwitchItemContent(
                checked = enabled,
                title = "Lost recent blog entries",
                description = stringResource(id = R.string.community_settings_cf_lost_description),
                onCheckedChange = { checked ->
                    scope.launch {
                        settings.codeforcesLostEnabled.setValue(checked)
                        with(CodeforcesCommunityLostRecentWorker.getWork(context)) {
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
    item: DataStoreItem<CodeforcesColorTag>
) {
    val options = remember {
        listOf(
            CodeforcesColorTag.BLACK to "Exists",
            CodeforcesColorTag.GRAY to "Newbie",
            CodeforcesColorTag.GREEN to "Pupil",
            CodeforcesColorTag.CYAN to "Specialist",
            CodeforcesColorTag.BLUE to "Expert",
            CodeforcesColorTag.VIOLET to "Candidate Master",
            CodeforcesColorTag.ORANGE to "Master",
            CodeforcesColorTag.RED to "Grandmaster",
            CodeforcesColorTag.LEGENDARY to "LGM"
        )
    }
    //TODO: restart worker on change?
    Box(modifier = Modifier.padding(top = 10.dp)) {
        SettingsEnumItemContent(
            item = item,
            title = "Author at least",
            options = options.map { it.first },
            optionToString = { tag ->
                CodeforcesHandle(
                    handle = options.first { it.first == tag }.second,
                    colorTag = tag
                ).toHandleSpan()
            }
        )
    }
}

@Composable
private fun CodeforcesRuEnabledSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val locale by collectItemAsState { context.settingsCommunity.codeforcesLocale }

    SettingsSwitchItem(
        title = "Russian content",
        checked = locale == CodeforcesLocale.RU,
        onCheckedChange = { checked ->
            scope.launch {
                context.settingsCommunity.codeforcesLocale.setValue(
                    value = if (checked) CodeforcesLocale.RU else CodeforcesLocale.EN
                )
            }
        }
    )
}


private val NewsFeed.title: String get() =
    when (this) {
        NewsFeed.atcoder_news -> "AtCoder news"
        NewsFeed.project_euler_news -> "Project Euler news"
        NewsFeed.project_euler_problems -> "Project Euler recent problems"
    }

private val NewsFeed.shortName: String get() =
    when (this) {
        NewsFeed.atcoder_news -> "atcoder"
        NewsFeed.project_euler_news -> "pe_news"
        NewsFeed.project_euler_problems -> "pe_problems"
    }

private val NewsFeed.link: String get() =
    when (this) {
        NewsFeed.atcoder_news -> "atcoder.jp"
        NewsFeed.project_euler_news -> "projecteuler.net/news"
        NewsFeed.project_euler_problems -> "projecteuler.net/recent"
    }


@Composable
private fun NewsFeedsSettingsItem() {
    val context = context
    val enabledSettingsItem = remember { context.settingsCommunity.enabledNewsFeeds }

    val title = "Subscriptions"
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
            options = NewsFeed.entries,
            selectedOptions = remember { runBlocking { enabledSettingsItem() } },
            optionTitle = {
                Column {
                    Text(it.title)
                    Text(it.link, color = cpsColors.contentAdditional, fontSize = 15.sp)
                }
            },
            onDismissRequest = { showDialog = false },
            onSaveSelected = { current ->
                scope.launch {
                    val newSelectedFeeds = current - enabledSettingsItem()
                    enabledSettingsItem.setValue(current)
                    val pe_recent = NewsFeed.project_euler_problems
                    if ((newSelectedFeeds - pe_recent).isNotEmpty()) {
                        NewsWorker.getWork(context).startImmediate()
                    }
                    if (pe_recent in newSelectedFeeds) {
                        ProjectEulerRecentProblemsWorker.getWork(context).startImmediate()
                    }
                }
            }
        )
    }
}

@Composable
private fun RenderAllTabs() {
    val context = context
    val scope = rememberCoroutineScope()
    val settings = context.settingsCommunity
    SettingsSwitchItem(
        item = settings.renderAllTabs,
        title = "Render all tabs"
    ) {
        scope.launch {
            settings.renderAllTabs.setValue(it)
        }
    }
}