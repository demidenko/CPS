package com.demich.cps.contests

import androidx.compose.runtime.Composable
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.filter.FilterIconButton
import com.demich.cps.ui.filter.FilterState
import com.demich.cps.utils.LoadingStatus

@Composable
fun ContestsBottomBar(
    viewState: ContestsListViewState,
    filterState: FilterState,
    anyPlatformEnabled: Boolean,
    loadingStatus: LoadingStatus?,
    onReloadClick: () -> Unit
) {
    if (anyPlatformEnabled) {
        FilterIconButton(filterState = filterState)
    }

    if (anyPlatformEnabled) {
        ContestsPageSwitchButton(
            contestsPage = viewState.contestsPage,
            onClick = {
                viewState.contestsPage = it
            }
        )
    }

    CPSReloadingButton(
        loadingStatus = loadingStatus,
        enabled = anyPlatformEnabled,
        onClick = onReloadClick
    )
}

@Composable
private fun ContestsPageSwitchButton(
    contestsPage: ContestsListViewState.ContestsPage,
    onClick: (ContestsListViewState.ContestsPage) -> Unit
) {
    CPSIconButton(
        icon = CPSIcons.Swap,
        onClick = {
            onClick(
                when (contestsPage) {
                    Finished -> RunningOrUpcoming
                    RunningOrUpcoming -> Finished
                }
            )
        }
    )
}