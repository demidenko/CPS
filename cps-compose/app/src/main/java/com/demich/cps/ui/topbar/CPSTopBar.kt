package com.demich.cps.ui.topbar

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSDropdownMenuButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.dialogs.CPSAboutDialog
import com.demich.cps.ui.theme.cpsColors

@Composable
fun CPSTopBar(
    subtitle: () -> String,
    additionalMenu: CPSMenuBuilder?,
) {
    var showUIPanel by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        backgroundColor = cpsColors.background,
        elevation = 0.dp,
        contentPadding = PaddingValues(start = 10.dp),
        modifier = Modifier.height(56.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Title(
                subtitle = subtitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = showUIPanel,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                UIPanel(modifier = Modifier.fillMaxWidth()) { showUIPanel = false }
            }
        }

        CPSDropdownMenuButton(
            icon = CPSIcons.More,
            color = cpsColors.content,
        ) {
            CPSDropdownMenuItem(title = "UI", icon = CPSIcons.SettingsUI) {
                showUIPanel = true
            }
            CPSDropdownMenuItem(title = "About", icon = CPSIcons.Info) {
                showAbout = true
            }
            additionalMenu?.let {
                Divider(color = cpsColors.divider)
                it()
            }
        }
    }

    if (showAbout) CPSAboutDialog(onDismissRequest = { showAbout = false })
}


