package com.demich.cps

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSDropdownMenuButton
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.StatusBarButtonsForUIPanel
import com.demich.cps.ui.dialogs.CPSAboutDialog
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.DarkLightMode
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

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
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
            CPSTitle(
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


@Composable
private fun CPSTitle(
    subtitle: () -> String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp
) {
    ProvideTextStyle(
        value = CPSDefaults.MonospaceTextStyle.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    ) {
        Column(modifier = modifier) {
            Text(
                text = "Competitive Programming && Solving",
                color = cpsColors.content,
                maxLines = 1
            )
            Text(
                text = subtitle(),
                color = cpsColors.contentAdditional,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UIPanel(
    modifier: Modifier = Modifier,
    onClosePanel: () -> Unit
) = Row(modifier = modifier.background(cpsColors.background)) {
    val scope = rememberCoroutineScope()

    val settingsUI = with(context) { remember { settingsUI } }
    val useOriginalColors by rememberCollect { settingsUI.useOriginalColors.flow }
    val darkLightMode by rememberCollect { settingsUI.darkLightMode.flow }

    CPSIconButton(icon = CPSIcons.Close, onClick = onClosePanel)
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CPSIconButton(icon = CPSIcons.Colors, onState = useOriginalColors) {
            scope.launch {
                settingsUI.useOriginalColors(!useOriginalColors)
            }
        }
        StatusBarButtonsForUIPanel()
        DarkLightModeButton(
            mode = darkLightMode,
            isSystemInDarkMode = isSystemInDarkTheme()
        ) { mode ->
            scope.launch {
                settingsUI.darkLightMode(mode)
            }
        }
    }
}

@Composable
private fun DarkLightModeButton(
    mode: DarkLightMode,
    isSystemInDarkMode: Boolean,
    onModeChanged: (DarkLightMode) -> Unit
) {
    CPSIconButton(
        icon = when (mode) {
            DarkLightMode.SYSTEM -> CPSIcons.DarkLightAuto
            else -> CPSIcons.DarkLight
        },
        onClick = {
            onModeChanged(
                when (mode) {
                    DarkLightMode.SYSTEM -> if (isSystemInDarkMode) DarkLightMode.LIGHT else DarkLightMode.DARK
                    DarkLightMode.DARK -> DarkLightMode.LIGHT
                    DarkLightMode.LIGHT -> DarkLightMode.DARK
                }
            )
        }
    )
}

