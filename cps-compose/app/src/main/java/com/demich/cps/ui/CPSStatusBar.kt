package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.*
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.navigation.Screen
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.datastore_itemized.edit
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CPSStatusBar(
    systemUiController: SystemUiController,
    currentScreen: Screen?
) {
    val context = context
    val coloredStatusBar by rememberCollect { context.settingsUI.coloredStatusBar.flow }
    val rankGetter by rememberCollect { makeFlowOfRankGetter(context) }
    ColorizeStatusBar(
        systemUiController = systemUiController,
        coloredStatusBar = coloredStatusBar,
        rank = rankGetter[currentScreen]
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

private fun<U: RatedUserInfo> RatedAccountManager<U>.getRank(userInfo: U): RatedRank? {
    val rating = userInfo.rating ?: return null
    val handleColor = getHandleColor(rating)
    if(handleColor == HandleColor.RED) return RatedRank(rank = 1e9, handleColor = handleColor, manager = this)
    val i = rankedHandleColorsList.indexOfFirst { handleColor == it }
    val j = rankedHandleColorsList.indexOfLast { handleColor == it }
    val pos = ratingsUpperBounds.indexOfFirst { it.first == handleColor }
    require(i != -1 && j >= i && pos != -1)
    val lower = if(pos > 0) ratingsUpperBounds[pos-1].second else 0
    val upper = ratingsUpperBounds[pos].second
    val blockLength = (upper - lower).toDouble() / (j - i + 1)
    return RatedRank(
        rank = i + (rating - lower) / blockLength,
        handleColor = handleColor,
        manager = this
    )
}


private fun<U: RatedUserInfo> RatedAccountManager<U>.flowOfRatedRank(): Flow<RatedRank?> =
    flowOfInfo().map(this::getRank)

private data class RankGetter(
    private val validRanks: List<RatedRank>,
    private val resultByMaximum: Boolean
) {
    operator fun get(screen: Screen?): RatedRank? {
        return when (screen) {
            is Screen.AccountExpanded -> validRanks.find { it.manager.type == screen.type }
            is Screen.AccountSettings -> validRanks.find { it.manager.type == screen.type }
            else -> {
                if (resultByMaximum) validRanks.maxByOrNull { it.rank }
                else validRanks.minByOrNull { it.rank }
            }
        }
    }
}

private fun makeFlowOfRankGetter(context: Context): Flow<RankGetter> =
    combine(
        flow = combine(flows = context.allAccountManagers
            .filterIsInstance<RatedAccountManager<out RatedUserInfo>>()
            .map { it.flowOfRatedRank() }
        ) { it },
        flow2 = context.settingsUI.statusBarDisabledManagers.flow,
        flow3 = context.settingsUI.statusBarResultByMaximum.flow
    ) { ranks, disabledManagers, resultByMaximum ->
        RankGetter(
            validRanks = ranks.filterNotNull().filter { it.manager.type !in disabledManagers },
            resultByMaximum = resultByMaximum
        )
    }.distinctUntilChanged()

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
    val koef by animateFloatAsState(
        targetValue = if (isStatusBarEnabled) 1f else 0f,
        animationSpec = tween(CPSDefaults.buttonOnOffDurationMillis)
    )
    val statusBarColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(CPSDefaults.buttonOnOffDurationMillis)
    )
    systemUiController.setStatusBarColor(
        color = lerp(offColor, statusBarColor, koef),
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
        combine(flows = context.allAccountManagers
            .filterIsInstance<RatedAccountManager<*>>()
            .map { it.flowOfInfoWithManager() }
        ) { array ->
            array.mapNotNull { it?.manager }
        }.combine(settingsUI.flowOfAccountsOrder()) { managers, order ->
            managers.sortedBy { order.indexOf(it.type) }
        }
    }

    var showPopup by rememberSaveable { mutableStateOf(false) }
    val resultByMaximum by rememberCollect { settingsUI.statusBarResultByMaximum.flow }
    val disabledManagers by rememberCollect { settingsUI.statusBarDisabledManagers.flow }

    val noneEnabled by remember {
        derivedStateOf {
            recordedAccountManagers.all { it.type in disabledManagers }
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
    disabledManagers: Set<AccountManagers>,
    onCheckedChange: (AccountManagers, Boolean) -> Unit,
    accountManagers: List<AccountManager<out RatedUserInfo>>,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(cpsColors.backgroundAdditional)
    ) {
        accountManagers.forEach { manager ->
            Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CPSCheckBox(
                    checked = manager.type !in disabledManagers,
                    onCheckedChange = { checked -> onCheckedChange(manager.type, checked) }
                )
                MonospacedText(text = manager.type.name)
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