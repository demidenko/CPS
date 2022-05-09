package com.demich.cps.ui

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.demich.cps.Screen
import com.demich.cps.getScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Composable
fun rememberCPSNavigator(
    navController: NavController
): CPSNavigator {
    val subtitleState: MutableState<String> = remember {
        mutableStateOf("")
    }
    val currentScreenState: State<Screen?> = remember {
        navController.currentBackStackEntryFlow.map { it.getScreen() }
            .onEach {
                subtitleState.value = it.subtitle
            }
    }.collectAsState(initial = null)
    return CPSNavigator(
        navController = navController,
        currentScreenState = currentScreenState,
        subtitleState = subtitleState
    )
}

@Stable
class CPSNavigator(
    private val navController: NavController,
    private val subtitleState: MutableState<String>,
    val currentScreenState: State<Screen?>
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
}