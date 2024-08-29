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
import com.demich.cps.R
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.community.codeforces.CodeforcesTitle
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.api.CodeforcesLocale
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
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberWith
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
    val requiredPermissions by rememberWith(context) {
        flowOfNotificationPermissionsRequired(this)
    }.collectAsState(initial = false)

    SettingsColumn(
        requiredNotificationsPermission = requiredPermissions,
        modifier = Modifier.fillMaxHeight()
    ) {
        SettingsSectionHeader(
            title = "codeforces",
            painter = platformIconPainter(platform = Contest.Platform.codeforces)
        )
        CodeforcesDefaultTabSettingsItem()
        CodeforcesFollowSettingsItem()
        CodeforcesLostSettingsItem()
        CodeforcesRuEnabledSettingsItem()

        SettingsSectionHeader(
            title = "news feeds",
            painter = rememberVectorPainter(image = CPSIcons.NewsFeeds)
        )
        NewsFeedsSettingsItem()
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
                        settings.codeforcesLostEnabled(newValue = checked)
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
                context.settingsCommunity.codeforcesLocale(
                    newValue = if (checked) CodeforcesLocale.RU else CodeforcesLocale.EN
                )
            }
        }
    )
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
            options = CommunitySettingsDataStore.NewsFeed.entries,
            selectedOptions = remember { runBlocking { enabledSettingsItem() } },
            optionTitle = { Text(it.link) },
            onDismissRequest = { showDialog = false },
            onSaveSelected = { current ->
                scope.launch {
                    val newSelectedFeeds = current - enabledSettingsItem()
                    enabledSettingsItem(newValue = current)
                    val pe_recent = CommunitySettingsDataStore.NewsFeed.project_euler_problems
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