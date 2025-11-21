package com.demich.cps.community.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.demich.cps.R
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.community.codeforces.CodeforcesTitle
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed
import com.demich.cps.contests.database.Contest
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenStaticTitleState
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.platformIconPainter
import com.demich.cps.ui.settings.Item
import com.demich.cps.ui.settings.MultiSelectEnum
import com.demich.cps.ui.settings.SelectEnum
import com.demich.cps.ui.settings.SettingsColumn
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.settings.SettingsSectionHeader
import com.demich.cps.ui.settings.Switch
import com.demich.cps.ui.settings.SwitchByItem
import com.demich.cps.ui.settings.SwitchByWork
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
import com.demich.datastore_itemized.setValueIn
import com.demich.datastore_itemized.value
import kotlinx.coroutines.flow.Flow


@Composable
private fun CommunitySettingsScreen() {
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
            DefaultTabSettingsItem()
            FollowSettingsItem()
            LostSettingsItem()
            RuEnabledSettingsItem()
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

@Composable
fun CPSNavigator.ScreenScope<Screen.CommunitySettings>.NavContentCommunitySettingsScreen() {
    screenTitle = ScreenStaticTitleState("community", "settings")

    CommunitySettingsScreen()
}

private fun flowOfNotificationPermissionsRequired(context: Context): Flow<Boolean> =
    context.settingsCommunity.flowOf {
        codeforcesFollowEnabled.value || enabledNewsFeeds.value.isNotEmpty()
    }

@Composable
context(scope: SettingsContainerScope)
private fun DefaultTabSettingsItem() {
    val context = context
    SelectEnum(
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
context(scope: SettingsContainerScope)
private fun FollowSettingsItem() {
    val context = context
    SwitchByWork(
        item = context.settingsCommunity.codeforcesFollowEnabled,
        title = "Follow",
        description = stringResource(id = R.string.community_settings_cf_follow_description),
        workProvider = CodeforcesCommunityFollowWorker
    )
}

@Composable
context(scope: SettingsContainerScope)
private fun LostSettingsItem() {
    val context = context
    val settings = remember { context.settingsCommunity }
    val item = remember { settings.codeforcesLostEnabled }
    val enabled by collectItemAsState { item }
    Item {
        SwitchByWork(
            item = item,
            title = "Lost recent blog entries",
            description = stringResource(id = R.string.community_settings_cf_lost_description),
            workProvider = CodeforcesCommunityLostRecentWorker
        )
        AnimatedVisibility(
            visible = enabled,
            // TODO: copy pasted from ColumnScope.AnimatedVisibility
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            LostAuthorSettingsItem(item = settings.codeforcesLostMinRatingTag)
        }
    }
}

@Composable
context(scope: SettingsContainerScope)
private fun LostAuthorSettingsItem(
    item: DataStoreItem<CodeforcesColorTag>
) {
    CodeforcesColorTag.entries
    val names = remember {
        mapOf(
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
    SelectEnum(
        item = item,
        title = "Author at least",
        options = names.keys,
        optionTitle = { tag ->
            Text(text = CodeforcesHandle(handle = names.getValue(tag), colorTag = tag).toHandleSpan())
        }
    )
}

@Composable
context(scope: SettingsContainerScope)
private fun RuEnabledSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val locale by collectItemAsState { context.settingsCommunity.codeforcesLocale }

    Switch(
        title = "Russian content",
        checked = locale == CodeforcesLocale.RU,
        onCheckedChange = { checked ->
            context.settingsCommunity.codeforcesLocale.setValueIn(
                scope = scope,
                value = if (checked) CodeforcesLocale.RU else CodeforcesLocale.EN
            )
        }
    )
}


private val NewsFeed.title: String
    get() = when (this) {
        NewsFeed.atcoder_news -> "AtCoder news"
        NewsFeed.project_euler_news -> "Project Euler news"
        NewsFeed.project_euler_problems -> "Project Euler recent problems"
    }

private val NewsFeed.shortName: String
    get() = when (this) {
        NewsFeed.atcoder_news -> "atcoder"
        NewsFeed.project_euler_news -> "pe_news"
        NewsFeed.project_euler_problems -> "pe_problems"
    }

private val NewsFeed.link: String
    get() = when (this) {
        NewsFeed.atcoder_news -> "atcoder.jp"
        NewsFeed.project_euler_news -> "projecteuler.net/news"
        NewsFeed.project_euler_problems -> "projecteuler.net/recent"
    }


@Composable
context(scope: SettingsContainerScope)
private fun NewsFeedsSettingsItem() {
    val context = context
    val enabledItem = remember { context.settingsCommunity.enabledNewsFeeds }

    MultiSelectEnum(
        title = "Subscriptions",
        item = enabledItem,
        options = NewsFeed.entries,
        optionName = { it.shortName },
        optionContent = {
            Column {
                Text(text = it.title)
                Text(
                    text = it.link,
                    color = cpsColors.contentAdditional,
                    fontSize = CPSFontSize.settingsSubtitle
                )
            }
        },
        onNewSelected = {
            val pe_recent = NewsFeed.project_euler_problems
            if ((it - pe_recent).isNotEmpty()) {
                NewsWorker.getWork(context).startImmediate()
            }
            if (pe_recent in it) {
                ProjectEulerRecentProblemsWorker.getWork(context).startImmediate()
            }
        }
    )
}

@Composable
context(scope: SettingsContainerScope)
private fun RenderAllTabs() {
    val context = context
    SwitchByItem(
        item = context.settingsCommunity.renderAllTabs,
        title = "Render all tabs"
    )
}