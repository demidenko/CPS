package com.demich.cps.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.demich.cps.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@Composable
fun rememberCPSNavigator(
    navController: NavHostController
): CPSNavigator {
    val subtitleState = remember { mutableStateOf("") }

    val currentScreenState: State<Screen?>
        = remember(navController) {
            navController.currentBackStackEntryFlow.map { it.getScreen() }
        }.collectAsState(initial = null)

    val menuBuilderState = remember { mutableStateOf<CPSMenuBuilder?>(null) }
    val bottomBarBuilderState = remember { mutableStateOf<AdditionalBottomBarBuilder?>(null) }

    return CPSNavigator(
        navController = navController,
        currentScreenState = currentScreenState,
        subtitleState = subtitleState,
        menuBuilderState = menuBuilderState,
        bottomBarBuilderState = bottomBarBuilderState
    )
}

@Stable
class CPSNavigator(
    private val navController: NavHostController,
    private val subtitleState: MutableState<String>,
    private val currentScreenState: State<Screen?>,
    private val menuBuilderState: MutableState<CPSMenuBuilder?>,
    private val bottomBarBuilderState: MutableState<AdditionalBottomBarBuilder?>
) {

    val subtitle: String
        get() = subtitleState.value

    //TODO: each LaunchEffect with this is slow shit
    fun setSubtitle(vararg words: String) {
        subtitleState.value = words.joinToString(prefix = "::", separator = ".") { it.lowercase() }
    }

    val currentScreen: Screen?
        get() = currentScreenState.value

    val isBottomBarEnabled: Boolean
        get() = currentScreen.let {
            it == null || it.enableBottomBar
        }

    fun navigateTo(screen: Screen) {
        val currentScreen = currentScreenState.value ?: return
        if (screen.rootScreenType != currentScreen.rootScreenType) {
            //switch stack
            navController.navigate(route = screen.routePath) {
                popUpTo(currentScreen.rootScreenType.route) {
                    saveState = true
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            if (screen != currentScreen) {
                navController.navigate(route = screen.routePath)
            }
        }
    }

    fun popBack() {
        navController.popBackStack()
    }

    inner class DuringCompositionHolder(
        val screen: Screen
    ) {
        var menu: CPSMenuBuilder?
            get() = menuBuilderState.value
            set(value) { if (screen == currentScreen) menuBuilderState.value = value }

        var bottomBar: AdditionalBottomBarBuilder?
            get() = bottomBarBuilderState.value
            set(value) { if (screen == currentScreen) bottomBarBuilderState.value = value }

        val menuSetter: (CPSMenuBuilder) -> Unit get() = { menu = it }
        val bottomBarSetter: (AdditionalBottomBarBuilder) -> Unit get() = { bottomBar = it }
    }

    @Composable
    fun NavHost(
        modifier: Modifier = Modifier,
        builder: NavGraphBuilder.() -> Unit
    ) {
        val startRoute = with(context) {
            remember { runBlocking { settingsUI.startScreenRoute() } }
        }
        androidx.navigation.compose.NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = modifier,
            builder = builder
        )
    }

    @Composable
    fun TopBar() {
        CPSTopBar(
            navigator = this,
            additionalMenu = menuBuilderState.value
        )
    }

    @Composable
    fun BottomBar() {
        CPSBottomBar(
            navigator = this,
            additionalBottomBar = bottomBarBuilderState.value
        )
    }


    @Composable
    fun ColorizeNavAndStatusBars(
        systemUiController: SystemUiController = rememberSystemUiController()
    ) {
        systemUiController.setNavigationBarColor(
            color = cpsColors.backgroundNavigation,
            darkIcons = MaterialTheme.colors.isLight
        )

        CPSStatusBar(
            systemUiController = systemUiController,
            currentScreen = currentScreen
        )
    }
}