package com.demich.cps.community

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.demich.cps.community.codeforces.CodeforcesCommunityBackgroundLoader
import com.demich.cps.community.codeforces.CodeforcesCommunityBottomBar
import com.demich.cps.community.codeforces.CodeforcesCommunityController
import com.demich.cps.community.codeforces.CodeforcesCommunityPagerState
import com.demich.cps.community.codeforces.CodeforcesCommunityScreen
import com.demich.cps.community.codeforces.codeforcesCommunityViewModel
import com.demich.cps.community.codeforces.rememberCodeforcesCommunityPagerState
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.CodeforcesCommunityScreenSpecs
import com.demich.cps.community.settings.codeforcesScreenSpecs
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesFollowRepository
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTitleState
import com.demich.cps.navigation.cpsScreenTitleOf
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberRunBlocking

@Composable
fun CPSNavigator.ScreenScope<Screen.Community>.NavContentCommunityScreen(
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
) {
    val context = context
    val followRepository = remember { context.followRepository }

    val viewModel = codeforcesCommunityViewModel()

    // TODO: collectUntilFirst make bottombar glitch
    val specs = rememberRunBlocking { context.settingsCommunity.codeforcesScreenSpecs() }

    NavContentCommunityScreen(
        specs = specs,
        followRepository = followRepository,
        backgroundLoader = viewModel,
        onOpenSettings = onOpenSettings,
        onOpenFollowList = onOpenFollowList
    )
}

@Composable
private fun CPSNavigator.ScreenScope<Screen.Community>.NavContentCommunityScreen(
    specs: CodeforcesCommunityScreenSpecs,
    followRepository: CodeforcesFollowRepository,
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
) {
    val pagerState = rememberCodeforcesCommunityPagerState(
        defaultTab = specs.defaultTab,
        lostEnabled = specs.lostEnabled
    )

    val controller = remember(pagerState, backgroundLoader) {
        CodeforcesCommunityController(
            backgroundLoader = backgroundLoader,
            pagerState = pagerState
        )
    }

    val context = context
    SideEffect(controller) {
        controller.touchFlows(context = context, pagerState = pagerState)
    }

    screenTitle = remember(pagerState) {
        ScreenTitleState {
            cpsScreenTitleOf("community", "codeforces", pagerState.currentTab.name)
        }
    }

    menu = communityMenuBuilder(
        followEnabled = specs.followEnabled,
        onOpenSettings = onOpenSettings,
        onOpenFollowList = {
            controller.updateFollowUsersInfo(repository = followRepository) // TODO: not proper place to call?
            onOpenFollowList()
        }
    )

    CommunityScreen(
        controller = controller,
        renderAllPages = specs.renderAllPages
    )

    bottomBar = communityBottomBarBuilder(
        pagerState = pagerState,
        backgroundLoader = controller
    )
}

@Composable
private fun CommunityScreen(
    controller: CodeforcesCommunityController,
    renderAllPages: Boolean
) {
    CodeforcesCommunityScreen(
        controller = controller,
        renderAllPages = renderAllPages
    )
}

private fun communityBottomBarBuilder(
    pagerState: CodeforcesCommunityPagerState,
    backgroundLoader: CodeforcesCommunityBackgroundLoader
): AdditionalBottomBarBuilder = {
    val context = context

    CodeforcesCommunityBottomBar(pagerState = pagerState)

    val loadingStatus by backgroundLoader.loadingStatusState()
    CPSReloadingButton(
        loadingStatus = loadingStatus,
        onClick = {
            backgroundLoader.reload(tabs = pagerState.visitedTabs, context = context)
        }
    )
}

private fun communityMenuBuilder(
    followEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
): CPSMenuBuilder = {
    CPSDropdownMenuItem(
        title = "Settings",
        icon = CPSIcons.Settings,
        onClick = onOpenSettings
    )

    if (followEnabled) {
        CPSDropdownMenuItem(
            title = "Follow List",
            icon = CPSIcons.Profiles,
            onClick = onOpenFollowList
        )
    }
}

@Composable
private fun CodeforcesCommunityBackgroundLoader.loadingStatusState(): State<LoadingStatus?> =
    remember { flowOfLoadingStatus() }.collectAsState(initial = null)

private fun CodeforcesCommunityBackgroundLoader.touchFlows(
    context: Context,
    pagerState: CodeforcesCommunityPagerState
) {
    pagerState.visitedTabs.forEach { tab ->
        when (tab) {
            MAIN -> flowOfMainBlogEntries(context)
            TOP -> {
                when (pagerState.topPageType) {
                    BlogEntries -> flowOfTopBlogEntries(context)
                    Comments -> flowOfTopComments(context)
                }
            }
            RECENT -> flowOfRecent(context)
            else -> { }
        }
    }
}