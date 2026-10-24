package com.demich.cps.community

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.demich.cps.community.codeforces.CodeforcesCommunityBackgroundLoader
import com.demich.cps.community.codeforces.CodeforcesCommunityBottomBar
import com.demich.cps.community.codeforces.CodeforcesCommunityPagerState
import com.demich.cps.community.codeforces.CodeforcesCommunityScreen
import com.demich.cps.community.codeforces.CodeforcesCommunityViewModel
import com.demich.cps.community.codeforces.CodeforcesTab
import com.demich.cps.community.codeforces.codeforcesCommunityViewModel
import com.demich.cps.community.codeforces.reload
import com.demich.cps.community.codeforces.rememberCodeforcesCommunityPagerState
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.CodeforcesCommunityScreenSpecs
import com.demich.cps.community.settings.codeforcesScreenSpecs
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.ScreenTitleState
import com.demich.cps.navigation.cpsScreenTitleOf
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberRunBlocking
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Composable
fun CPSNavigator.ScreenScope<Screen.Community>.NavContentCommunityScreen(
    navigateToSettings: () -> Unit,
    navigateToFollowList: () -> Unit
) {
    val context = context

    val viewModel = codeforcesCommunityViewModel()
    val backgroundLoader = remember(viewModel) {
        CodeforcesCommunityBackgroundLoader(viewModel, context)
    }

    // TODO: collectUntilFirst make bottombar glitch
    val specs = rememberRunBlocking { context.settingsCommunity.codeforcesScreenSpecs() }

    NavContentCommunityScreen(
        specs = specs,
        backgroundLoader = backgroundLoader,
        onOpenSettings = navigateToSettings,
        onOpenFollowList = {
            viewModel.updateFollowUsersInfo(repository = context.followRepository) // TODO: not proper place to call?
            navigateToFollowList()
        }
    )
}

@Composable
private fun CPSNavigator.ScreenScope<Screen.Community>.NavContentCommunityScreen(
    specs: CodeforcesCommunityScreenSpecs,
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    onOpenSettings: () -> Unit,
    onOpenFollowList: () -> Unit
) {
    val pagerState = rememberCodeforcesCommunityPagerState(
        defaultTab = specs.defaultTab,
        lostEnabled = specs.lostEnabled
    )

    SideEffect(backgroundLoader, pagerState) {
        backgroundLoader.touchFlows(pagerState = pagerState)
    }

    screenTitle = remember(pagerState) {
        ScreenTitleState {
            cpsScreenTitleOf("community", "codeforces", pagerState.currentTab.name)
        }
    }

    menu = communityMenuBuilder(
        followEnabled = specs.followEnabled,
        onOpenSettings = onOpenSettings,
        onOpenFollowList = onOpenFollowList
    )

    CodeforcesCommunityScreen(
        backgroundLoader = backgroundLoader,
        pagerState = pagerState,
        renderAllPages = specs.renderAllPages
    )

    bottomBar = communityBottomBarBuilder(
        pagerState = pagerState,
        backgroundLoader = backgroundLoader
    )
}

private fun communityBottomBarBuilder(
    pagerState: CodeforcesCommunityPagerState,
    backgroundLoader: CodeforcesCommunityBackgroundLoader
): AdditionalBottomBarBuilder = {
    CodeforcesCommunityBottomBar(pagerState = pagerState)

    val loadingStatus by backgroundLoader.loadingStatusState()
    CPSReloadingButton(
        loadingStatus = loadingStatus,
        onClick = {
            backgroundLoader.reload(tabs = pagerState.visitedTabs)
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
    pagerState: CodeforcesCommunityPagerState
) {
    pagerState.visitedTabs.forEach { tab ->
        when (tab) {
            MAIN -> flowOfMainBlogEntries()
            TOP -> {
                when (pagerState.topPageType) {
                    BlogEntries -> flowOfTopBlogEntries()
                    Comments -> flowOfTopComments()
                }
            }
            RECENT -> flowOfRecent()
            else -> { }
        }
    }
}

private fun CodeforcesCommunityBackgroundLoader(
    viewModel: CodeforcesCommunityViewModel,
    context: Context
): CodeforcesCommunityBackgroundLoader = object : CodeforcesCommunityBackgroundLoader {
    private val provider = viewModel.viewModelScope.async(
        context = Dispatchers.Default,
        start = LAZY,
        block = {
            CodeforcesClient(locale = context.settingsCommunity.codeforcesLocale())
        }
    )

    override fun flowOfLoadingStatus() =
        viewModel.flowOfLoadingStatus()

    override fun flowOfLoadingStatus(tab: CodeforcesTab) =
        viewModel.flowOfLoadingStatus(tab = tab)

    override fun reload(tab: CodeforcesTab) =
        viewModel.reload(tab = tab, provider = provider::await)

    override fun flowOfMainBlogEntries() =
        viewModel.flowOfMainBlogEntries(provider = provider::await)

    override fun flowOfTopBlogEntries() =
        viewModel.flowOfTopBlogEntries(provider = provider::await)

    override fun flowOfTopComments() =
        viewModel.flowOfTopComments(provider = provider::await)

    override fun flowOfRecent() =
        viewModel.flowOfRecent(provider = provider::await)
}