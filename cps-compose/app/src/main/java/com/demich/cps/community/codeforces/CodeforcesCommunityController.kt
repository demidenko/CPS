package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.workers.CodeforcesLostRecentWorker
import com.demich.cps.workers.isRunning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json


@Composable
fun rememberCodeforcesCommunityController(
    defaultTab: CodeforcesTab,
    lostEnabled: Boolean
): CodeforcesCommunityController {
    val context = context
    val viewModel = codeforcesCommunityViewModel()

    val tabs = remember(lostEnabled) {
        buildList<CodeforcesTab> {
            add(MAIN)
            add(TOP)
            add(RECENT)
            if (lostEnabled) add(LOST)
        }
    }

    val controller = rememberSaveable(saver = controllerSaver(viewModel, tabs)) {
        CodeforcesCommunityController(
            backgroundLoader = viewModel,
            pagerState = CodeforcesCommunityPagerState(
                tabs = tabs,
                pagerData = CodeforcesCommunityPagerData(
                    selectedTab = defaultTab,
                    topPageType = BlogEntries,
                    recentPageType = RecentFeed
                )
            )
        )
    }

    SideEffect(controller) {
        controller.touchFlows(context)
    }

    return controller
}

@Stable
class CodeforcesCommunityController(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    val pagerState: CodeforcesCommunityPagerState
): CodeforcesCommunityBackgroundLoader by backgroundLoader {

}

@Composable
fun CodeforcesCommunityBackgroundLoader.loadingStatusState(): State<LoadingStatus?> =
    remember { flowOfLoadingStatus() }.collectAsState(initial = null)

@Composable
fun CodeforcesCommunityBackgroundLoader.loadingStatusState(tab: CodeforcesTab): State<LoadingStatus> {
    if (tab == LOST) {
        val context = context
        return remember {
            CodeforcesLostRecentWorker.getWork(context)
                .flowOfWorkInfo()
                .map { if (it.isRunning) LoadingStatus.LOADING else PENDING }
        }.collectAsStateWithLifecycle(initialValue = PENDING)
    }

    return remember(tab) { flowOfLoadingStatus(tab) }
        .collectAsState(initial = PENDING) //TODO: be sure this fake is ok
}


private fun controllerSaver(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    tabs: List<CodeforcesTab>
) = Saver<CodeforcesCommunityController, String>(
    save = {
        val pagerState = it.pagerState
        Json.encodeToString(CodeforcesCommunityPagerData(
            selectedTab = pagerState.currentTab,
            topPageType = pagerState.topPageType,
            recentPageType = pagerState.recentPageType
        ))
    },
    restore = {
        CodeforcesCommunityController(
            backgroundLoader = backgroundLoader,
            pagerState = CodeforcesCommunityPagerState(tabs = tabs, pagerData = Json.decodeFromString(it))
        )
    }
)

fun CodeforcesCommunityBackgroundLoader.flowOfEntryIds(tab: CodeforcesTab, context: Context): Flow<List<Int>>? {
    val flow = when (tab) {
        MAIN -> flowOfMainBlogEntries(context)
        LOST -> flowOfLostBlogEntries(context)
        else -> return null
    }
    return flow.map { it?.map { it.id } ?: [] }
}

private fun CodeforcesCommunityController.touchFlows(context: Context) {
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