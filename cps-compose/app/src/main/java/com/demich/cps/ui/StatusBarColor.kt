package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.navigation.AccountScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.flowOfCurrentScreen
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColor
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Composable
fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    navController: NavController
) {
    val context = context
    val coloredStatusBar by rememberCollect { context.settingsUI.coloredStatusBar.flow }
    val rank by rememberCollect {
        combine(
            flow = makeFlowOfRankGetter(context),
            flow2 = flow {
                emit(null)
                emitAll(navController.flowOfCurrentScreen())
            }
        ) { rankGetter, currentScreen -> rankGetter[currentScreen] }
    }
    systemUiController.setStatusBarColor(
        color = statusBarColor(
            coloredStatusBar = coloredStatusBar,
            rank = rank
        ),
        darkIcons = MaterialTheme.colors.isLight
    )
}

@Composable
private fun statusBarColor(
    coloredStatusBar: Boolean,
    rank: RatedRank?,
    offColor: Color = cpsColors.background
): Color {
    return statusBarColor(
        isStatusBarEnabled = coloredStatusBar && rank != null,
        color = rank?.run { manager.colorFor(handleColor) } ?: offColor,
        offColor = offColor
    )
}

@Composable
private fun statusBarColor(
    isStatusBarEnabled: Boolean,
    color: Color,
    offColor: Color,
    durationMillis: Int = CPSDefaults.buttonOnOffDurationMillis
): Color {
    /*
        Important:
        with statusbar=off switching dark/light mode MUST be as fast as everywhere else
    */
    val statusBarColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = durationMillis)
    )
    return animateColor(
        enabledColor = statusBarColor,
        disabledColor = offColor,
        enabled = isStatusBarEnabled,
        animationSpec = tween(durationMillis = durationMillis)
    )
}


@Immutable
private data class RatedRank(
    val rank: Double,
    val handleColor: HandleColor,
    val manager: RatedAccountManager<out RatedUserInfo>
)

private fun<U: RatedUserInfo> RatedAccountManager<U>.getRank(userInfo: U?): RatedRank? {
    val rating = userInfo?.rating ?: return null
    val handleColor = getHandleColor(rating)
    if(handleColor == HandleColor.RED) return RatedRank(rank = 1e9, handleColor = handleColor, manager = this)
    val i = rankedHandleColorsList.indexOfFirst { handleColor == it }
    val j = rankedHandleColorsList.indexOfLast { handleColor == it }
    val pos = ratingsUpperBounds.indexOfFirst { it.handleColor == handleColor }
    require(i != -1 && j >= i && pos != -1)
    val lower = if(pos > 0) ratingsUpperBounds[pos-1].ratingUpperBound else 0
    val upper = ratingsUpperBounds[pos].ratingUpperBound
    val blockLength = (upper - lower).toDouble() / (j - i + 1)
    return RatedRank(
        rank = i + (rating - lower) / blockLength,
        handleColor = handleColor,
        manager = this
    )
}


private fun<U: RatedUserInfo> RatedAccountManager<U>.flowOfRatedRank(context: Context): Flow<RatedRank?> =
    dataStore(context).flowOfInfo().map(this::getRank)

private class RankGetter(
    private val validRanks: List<RatedRank>,
    disabledManagers: Set<AccountManagerType>,
    resultByMaximum: Boolean
) {
    private val rank: RatedRank? =
        validRanks.filter { it.manager.type !in disabledManagers }.run {
            if (resultByMaximum) maxByOrNull { it.rank }
            else minByOrNull { it.rank }
        }

    operator fun get(screen: Screen?): RatedRank? =
        when (screen) {
            is AccountScreen -> validRanks.find { it.manager.type == screen.type }
            else -> rank
        }
}

private fun makeFlowOfRankGetter(context: Context): Flow<RankGetter> =
    combine(
        flow = combine(allRatedAccountManagers.map { it.flowOfRatedRank(context) }) { it },
        flow2 = context.settingsUI.statusBarDisabledManagers.flow,
        flow3 = context.settingsUI.statusBarResultByMaximum.flow
    ) { ranks, disabledManagers, resultByMaximum ->
        RankGetter(
            validRanks = ranks.filterNotNull(),
            disabledManagers = disabledManagers,
            resultByMaximum = resultByMaximum
        )
    }


