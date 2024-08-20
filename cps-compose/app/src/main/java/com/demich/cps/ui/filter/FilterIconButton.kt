package com.demich.cps.ui.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons

@Composable
fun FilterIconButton(
    filterState: FilterState,
    modifier: Modifier = Modifier
) {
    if (filterState.available && !filterState.enabled) {
        CPSIconButton(
            icon = CPSIcons.Search,
            onClick = { filterState.enabled = true },
            modifier = modifier
        )
    }
}