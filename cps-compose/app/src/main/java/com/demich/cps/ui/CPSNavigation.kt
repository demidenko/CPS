package com.demich.cps.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.demich.cps.*
import com.demich.cps.utils.context
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

@Composable
fun rememberCPSNavigator(
    navController: NavHostController
): CPSNavigator {
    val subtitleState = remember { mutableStateOf("") }

    val currentScreenState: State<Screen?>
        = remember(key1 = navController, key2 = subtitleState) {
            navController.currentBackStackEntryFlow
                .map { it.getScreen() }
                .onEach { subtitleState.value = it.subtitle }
        }.collectAsState(initial = null)

    val menuBuilderState = remember { mutableStateOf<CPSMenuBuilder?>(null) }
    val bottomBarBuilderState = remember { mutableStateOf<AdditionalBottomBarBuilder?>(null) }

    return remember(currentScreenState, menuBuilderState, bottomBarBuilderState) {
        CPSNavigator(
            navController = navController,
            currentScreenState = currentScreenState,
            subtitleState = subtitleState,
            menuBuilderState = menuBuilderState,
            bottomBarBuilderState = bottomBarBuilderState
        )
    }
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

    val currentScreen: Screen?
        get() = currentScreenState.value

    val isBottomBarEnabled: Boolean
        get() = currentScreen.let {
            it == null || it.enableBottomBar
        }

    fun navigateTo(screen: Screen) {
        val currentScreen = currentScreenState.value
        if (screen.rootScreen == currentScreen?.rootScreen) {
            if (screen == currentScreen) {
                //same screen
                subtitleState.value = screen.subtitle
            } else {
                navController.navigate(route = screen.routePath)
            }
        } else {
            //switch stack
            navController.navigate(route = screen.routePath) {
                //TODO: wtf it works?
                popUpTo(currentScreen!!.rootScreen.routePattern) {
                    saveState = true
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = true
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
            set(value) { if (screen == currentScreenState.value) menuBuilderState.value = value }

        var bottomBar: AdditionalBottomBarBuilder?
            get() = bottomBarBuilderState.value
            set(value) { if (screen == currentScreenState.value) bottomBarBuilderState.value = value }

        val menuSetter get() = menuBuilderState.component2()
        val bottomBarSetter get() = bottomBarBuilderState.component2()
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
}