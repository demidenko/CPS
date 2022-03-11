package com.demich.cps

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import kotlinx.coroutines.launch

@Composable
fun CPSTopBar(
    navController: NavHostController,
    currentBackStackEntry: NavBackStackEntry?
) {
    val currentScreen = currentBackStackEntry?.destination?.getScreen()

    var showUIPanel by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    @Composable
    fun CPSMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
        DropdownMenuItem(
            onClick = {
                showMenu = false
                onClick()
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(26.dp)
            )
            Text(text = title, modifier = Modifier.padding(end = 26.dp))
        }
    }

    TopAppBar(
        backgroundColor = cpsColors.background,
        elevation = 0.dp,
        contentPadding = PaddingValues(start = 10.dp, end = 4.dp),
        modifier = Modifier.height(56.dp)
    ) {
        if (showUIPanel) {
            UIPanel(modifier = Modifier.weight(1f)) { showUIPanel = false }
        } else {
            if (currentScreen != null) {
                CPSTitle(screen = currentScreen, modifier = Modifier.weight(1f))
            }
        }
        Box {
            IconButton(
                onClick = { showMenu = true },
                content = { Icon(Icons.Default.MoreVert, null) }
            )
            DropdownMenu(
                expanded = showMenu,
                modifier = Modifier.background(cpsColors.backgroundAdditional),
                onDismissRequest = { showMenu = false },
            ) {
                CPSMenuItem("UI", Icons.Filled.SettingsApplications) {
                    showUIPanel = true
                }
                CPSMenuItem("About", Icons.Outlined.Info) {
                    showAbout = true
                }
                if (currentScreen == Screen.News) {
                    Divider(color = cpsColors.dividerColor)
                    CPSMenuItem("Settings", Icons.Default.Settings) {
                        navController.navigate(Screen.NewsSettings.route)
                    }
                    CPSMenuItem("Follow List", Icons.Rounded.PeopleAlt) {
                        //TODO Open FollowList
                    }
                }
            }
        }
    }

    if (showAbout) CPSAboutDialog {
        showAbout = false
    }
}

@Composable
private fun CPSTitle(
    screen: Screen,
    modifier: Modifier = Modifier
) = Column(modifier = modifier) {
    val fontSize = 16.sp
    MonospacedText(
        text = "Competitive Programming && Solving",
        color = cpsColors.textColor,
        fontSize = fontSize,
        maxLines = 1
    )
    MonospacedText(
        text = screen.subtitle,
        color = cpsColors.textColorAdditional,
        fontSize = fontSize,
        maxLines = 1
    )
}

@Composable
private fun UIPanel(
    modifier: Modifier = Modifier,
    onClosePanel: () -> Unit
) = Row(modifier = modifier) {
    val settingsUI = context.settingsUI
    val scope = rememberCoroutineScope()

    @Composable
    fun DarkLightModeButton(mode: DarkLightMode, isSystemInDarkMode: Boolean) {
        CPSIconButton(
            icon = when (mode) {
                DarkLightMode.SYSTEM -> Icons.Default.BrightnessAuto
                else -> Icons.Default.BrightnessMedium
            }
        ) {
            scope.launch {
                settingsUI.darkLightMode(
                    when (mode) {
                        DarkLightMode.SYSTEM -> if (isSystemInDarkMode) DarkLightMode.LIGHT else DarkLightMode.DARK
                        DarkLightMode.DARK -> DarkLightMode.LIGHT
                        DarkLightMode.LIGHT -> DarkLightMode.DARK
                    }
                )
            }
        }
    }

    val useOriginalColors by settingsUI.useOriginalColors.collectAsState()
    val coloredStatusBar by settingsUI.coloredStatusBar.collectAsState()
    val darkLightMode by settingsUI.darkLightMode.collectAsState()

    CPSIconButton(icon = Icons.Default.Close, onClick = onClosePanel)
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CPSIconButton(icon = Icons.Default.ColorLens, onState = useOriginalColors) {
            scope.launch {
                settingsUI.useOriginalColors(!useOriginalColors)
            }
        }
        CPSIconButton(icon = Icons.Default.WebAsset, onState = coloredStatusBar) {
            scope.launch {
                settingsUI.coloredStatusBar(!coloredStatusBar)
            }
        }
        DarkLightModeButton(mode = darkLightMode, isSystemInDarkMode = isSystemInDarkTheme())
    }
}


@Composable
private fun CPSAboutDialog(onDismissRequest: () -> Unit) {
    val settingsDev = context.settingsDev
    val scope = rememberCoroutineScope()

    val devModeEnabled by settingsDev.devModeEnabled.collectAsState()
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