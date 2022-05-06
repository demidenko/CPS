package com.demich.cps

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun CPSTopBar(
    currentScreen: Screen?,
    additionalMenu: CPSMenuBuilder?,
) {
    var showUIPanel by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

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
                screen = currentScreen,
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

        ContentWithCPSDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            content = {
                IconButton(
                    onClick = { showMenu = true },
                    content = { Icon(CPSIcons.More, null) },
                )
            }
        ) {
            CPSDropdownMenuItem(title = "UI", icon = CPSIcons.SettingsUI) {
                showUIPanel = true
            }
            CPSDropdownMenuItem(title = "About", icon = CPSIcons.Info) {
                showAbout = true
            }
            additionalMenu?.let {
                Divider(color = cpsColors.dividerColor)
                it()
            }
        }
    }

    if (showAbout) CPSAboutDialog(onDismissRequest = { showAbout = false })
}


@Composable
private fun CPSTitle(
    screen: Screen?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) = Column(modifier = modifier) {
    MonospacedText(
        text = "Competitive Programming && Solving",
        color = cpsColors.textColor,
        fontSize = fontSize,
        maxLines = 1
    )
    MonospacedText(
        text = screen?.subtitle ?: "",
        color = cpsColors.textColorAdditional,
        fontSize = fontSize,
        maxLines = 1
    )
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

@Composable
private fun CPSAboutDialog(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    val settingsDev = context.settingsDev
    val devModeEnabled by rememberCollect { settingsDev.devModeEnabled.flow }
    var showDevModeLine by remember { mutableStateOf(devModeEnabled) }

    showDevModeLine = true //TODO enable by click pattern

    CPSDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismissRequest
    ) {
        MonospacedText("   Competitive")
        MonospacedText("   Programming")
        MonospacedText("&& Solving")
        MonospacedText("{")
        MonospacedText("   version = ${BuildConfig.VERSION_NAME}")
        if (showDevModeLine) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonospacedText("   dev_mode = $devModeEnabled", Modifier.weight(1f))
                CPSCheckBox(checked = devModeEnabled) { checked ->
                    scope.launch { settingsDev.devModeEnabled(checked) }
                }
            }
        }
        MonospacedText("}")
    }
}
