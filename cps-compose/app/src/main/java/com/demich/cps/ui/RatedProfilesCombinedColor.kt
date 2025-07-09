package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.navigation.CPSNavigator
import com.demich.cps.navigation.Screen
import com.demich.cps.utils.animateToggleColorAsState
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Composable
fun ratedProfilesColorState(
    navigator: CPSNavigator,
    disabledColor: Color
): State<Color> {
    // TODO: upstream recomposition trigger on [rank] or [cpsColors] change

    val context = context
    val rank by collectAsState {
        combine(
            flow = makeFlowOfRankGetter(context),
            flow2 = navigator.flowOfCurrentScreen()
        ) { rankGetter, currentScreen -> rankGetter[currentScreen] }
    }

    // TODO: assume !enabled is rank = null ?
    val ratedColorEnabled by collectItemAsState { context.settingsUI.coloredStatusBar }

    return colorState(
        enabled = ratedColorEnabled && rank != null,
        enabledColor = rank?.run { manager.colorFor(handleColor) } ?: disabledColor,
        disabledColor = disabledColor
    )
}

@Composable
private fun colorState(
    enabled: Boolean,
    enabledColor: Color,
    disabledColor: Color
): State<Color> {
    /*
        Important:
        with statusbar=off switching dark/light mode MUST be as fast as everywhere else
    */
    val statusBarColorState = animateColorAsState(
        targetValue = enabledColor,
        animationSpec = CPSDefaults.toggleAnimationSpec()
    )
    return animateToggleColorAsState(
        enabledColorState = statusBarColorState,
        disabledColor = disabledColor,
        isEnabled = enabled,
        animationSpec = CPSDefaults.toggleAnimationSpec()
    )
}


@Immutable
private data class RatedRank(
    val rank: Double,
    val handleColor: HandleColor,
    val manager: RatedAccountManager<out RatedUserInfo>
)

private fun <U: RatedUserInfo> RatedAccountManager<U>.getRank(profile: ProfileResult<U>?): RatedRank? {
    val rating = profile?.userInfoOrNull()?.rating ?: return null
    val handleColor = getHandleColor(rating)
    val rank = when (handleColor) {
        HandleColor.RED -> Double.POSITIVE_INFINITY
        else -> {
            val i = rankedHandleColors.indexOfFirst { handleColor == it }
            val j = rankedHandleColors.indexOfLast { handleColor == it }
            val pos = ratingsUpperBounds.indexOfFirst { it.handleColor == handleColor }
            check(i != -1 && j >= i && pos != -1)
            val lower = if (pos > 0) ratingsUpperBounds[pos-1].ratingUpperBound else 0
            val upper = ratingsUpperBounds[pos].ratingUpperBound
            val blockLength = (upper - lower).toDouble() / (j - i + 1)
            i + (rating - lower) / blockLength
        }
    }
    return RatedRank(
        rank = rank,
        handleColor = handleColor,
        manager = this
    )
}


private fun <U: RatedUserInfo> RatedAccountManager<U>.flowOfRatedRank(context: Context): Flow<RatedRank?> =
    dataStore(context).profile.asFlow().map { getRank(it) }

private class RankGetter(
    private val validRanks: List<RatedRank>,
    disabledManagers: Set<AccountManagerType>,
    rankSelector: UISettingsDataStore.StatusBarRankSelector
) {
    private val rank: RatedRank? =
        validRanks.filter { it.manager.type !in disabledManagers }.run {
            when (rankSelector) {
                UISettingsDataStore.StatusBarRankSelector.Min -> minByOrNull { it.rank }
                UISettingsDataStore.StatusBarRankSelector.Max -> maxByOrNull { it.rank }
            }
        }

    operator fun get(screen: Screen?): RatedRank? =
        when (screen) {
            is Screen.ProfileScreen -> validRanks.find { it.manager.type == screen.managerType }
            else -> rank
        }
}

private fun makeFlowOfRankGetter(context: Context): Flow<RankGetter> =
    combine(
        flow = combine(allRatedAccountManagers.map { it.flowOfRatedRank(context) }) { it }, //TODO: optimize
        flow2 = context.settingsUI.statusBarDisabledManagers.asFlow(),
        flow3 = context.settingsUI.statusBarRankSelector.asFlow()
    ) { ranks, disabledManagers, rankSelector ->
        RankGetter(
            validRanks = ranks.filterNotNull(),
            disabledManagers = disabledManagers,
            rankSelector = rankSelector
        )
    }


