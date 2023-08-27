package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.navigation.AccountScreen
import com.demich.cps.navigation.Screen
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.animateColor
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.datastore_itemized.edit
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CPSStatusBar(
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
    ColorizeStatusBar(
        systemUiController = systemUiController,
        coloredStatusBar = coloredStatusBar,
        rank = rank
    )
}

@Composable
private fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    coloredStatusBar: Boolean,
    rank: RatedRank?,
    offColor: Color = cpsColors.background
) {
    ColorizeStatusBar(
        systemUiController = systemUiController,
        isStatusBarEnabled = coloredStatusBar && rank != null,
        color = rank?.run { manager.colorFor(handleColor) } ?: offColor,
        offColor = offColor
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

@Composable
private fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    isStatusBarEnabled: Boolean,
    color: Color,
    offColor: Color
) {
    /*
        Important:
        with statusbar=off switching dark/light mode MUST be as fast as everywhere else
    */
    val statusBarColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(CPSDefaults.buttonOnOffDurationMillis)
    )
    systemUiController.setStatusBarColor(
        color = animateColor(
            enabledColor = statusBarColor,
            disabledColor = offColor,
            enabled = isStatusBarEnabled,
            animationSpec = tween(CPSDefaults.buttonOnOffDurationMillis)
        ),
        darkIcons = MaterialTheme.colors.isLight
    )
}


@Composable
fun StatusBarButtonsForUIPanel() {
    val scope = rememberCoroutineScope()

    val context = context
    val settingsUI = remember { context.settingsUI }

    val coloredStatusBar by rememberCollect { settingsUI.coloredStatusBar.flow }

    val recordedAccountManagers by rememberCollect {
        combine(allRatedAccountManagers.map { it.flowOfInfoWithManager(context) }) { array ->
            array.mapNotNull { it?.manager?.type }
        }.combine(settingsUI.accountsOrder.flow) { managers, order ->
            managers.sortedBy { order.indexOf(it) }
        }
    }

    var showPopup by rememberSaveable { mutableStateOf(false) }
    val resultByMaximum by rememberCollect { settingsUI.statusBarResultByMaximum.flow }
    val disabledManagers by rememberCollect { settingsUI.statusBarDisabledManagers.flow }

    val noneEnabled by remember {
        derivedStateOf {
            recordedAccountManagers.all { it in disabledManagers }
        }
    }

    if (recordedAccountManagers.isNotEmpty()) {
        CPSIconButton(
            icon = CPSIcons.StatusBar,
            onState = coloredStatusBar && !noneEnabled,
        ) {
            if (noneEnabled) {
                showPopup = true
            } else {
                scope.launch { settingsUI.coloredStatusBar(!coloredStatusBar) }
            }
        }
        if (coloredStatusBar) Box {
            CPSIconButton(
                icon = CPSIcons.MoveDown,
                onClick = { showPopup = true }
            )
            StatusBarAccountsPopup(
                expanded = showPopup,
                resultByMaximum = resultByMaximum,
                setResultByMaximum = {
                    scope.launch { settingsUI.statusBarResultByMaximum(it) }
                },
                disabledManagers = disabledManagers,
                onCheckedChange = { type, checked ->
                    scope.launch {
                        settingsUI.statusBarDisabledManagers.edit {
                            if (checked) remove(type) else add(type)
                        }
                    }
                },
                accountManagers = recordedAccountManagers,
                onDismissRequest = { showPopup = false }
            )
        }
    }

}


@Composable
private fun StatusBarAccountsPopup(
    expanded: Boolean,
    resultByMaximum: Boolean,
    setResultByMaximum: (Boolean) -> Unit,
    disabledManagers: Set<AccountManagerType>,
    onCheckedChange: (AccountManagerType, Boolean) -> Unit,
    accountManagers: List<AccountManagerType>,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(cpsColors.backgroundAdditional)
    ) {
        accountManagers.forEach { type ->
            Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CPSCheckBox(
                    checked = type !in disabledManagers,
                    onCheckedChange = { checked -> onCheckedChange(type, checked) }
                )
                MonospacedText(text = type.name)
            }
        }
        TextButtonsSelectRow(
            values = listOf(false, true),
            selectedValue = resultByMaximum,
            text = { value ->
                if (value) "best" else "worst"
            },
            onSelect = setResultByMaximum,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}