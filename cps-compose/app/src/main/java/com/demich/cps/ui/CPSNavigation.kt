package com.demich.cps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.demich.cps.CPSTopBar
import com.demich.cps.navigation.Screen
import com.demich.cps.navigation.getScreen
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.bottombar.CPSBottomBar
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberWith
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@Composable
fun rememberCPSNavigator(
    navController: NavHostController
): CPSNavigator {
    val subtitleState = remember { mutableStateOf("") }

    val currentScreenState: State<Screen?> =
        rememberWith(navController) { flowOfCurrentScreen() }.collectAsState(initial = null)

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

fun NavController.flowOfCurrentScreen(): Flow<Screen> =
    currentBackStackEntryFlow.map { it.getScreen() }

@Stable
class CPSNavigator(
    private val navController: NavHostController,
    currentScreenState: State<Screen?>,
    private val subtitleState: MutableState<String>,
    private val menuBuilderState: MutableState<CPSMenuBuilder?>,
    private val bottomBarBuilderState: MutableState<AdditionalBottomBarBuilder?>
) {
    val currentScreen by currentScreenState

    val isBottomBarEnabled: Boolean
        get() = currentScreen.let {
            it == null || it.enableBottomBar
        }

    fun navigateTo(screen: Screen) {
        val currentScreen = currentScreen ?: return
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

        fun setSubtitle(vararg words: String) {
            if (screen == currentScreen) {
                subtitleState.value = words.joinToString(prefix = "::", separator = ".") { it.lowercase() }
            }
        }
    }

    @Composable
    fun NavHost(
        modifier: Modifier = Modifier,
        builder: NavGraphBuilder.() -> Unit
    ) {
        val startRoute = rememberWith(context) {
            runBlocking { settingsUI.startScreenRoute() }
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
            subtitle = { subtitleState.value },
            additionalMenu = menuBuilderState.value
        )
    }

    @Composable
    fun BottomBar(systemUiController: SystemUiController) {
        CPSBottomBar(
            navigator = this,
            additionalBottomBar = bottomBarBuilderState.value,
            onSetSystemNavColor = systemUiController::setNavigationBarColor
        )
    }
}