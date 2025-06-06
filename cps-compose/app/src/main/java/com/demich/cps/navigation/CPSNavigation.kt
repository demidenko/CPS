package com.demich.cps.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.topbar.CPSTopBar
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

@Composable
fun rememberCPSNavigator(
    navController: NavHostController = rememberNavController()
): CPSNavigator {
    val subtitleState = remember { mutableStateOf("") }

    val currentScreenState: State<Screen?> =
        collectAsState { navController.flowOfCurrentScreen() }

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

private fun NavController.flowOfCurrentScreen(): Flow<Screen?> =
    flow {
        emit(null)
        currentBackStackEntryFlow.collect {
            emit(it.getScreen())
        }
    }

@Stable
class CPSNavigator(
    private val navController: NavHostController,
    currentScreenState: State<Screen?>,
    private val subtitleState: MutableState<String>,
    private val menuBuilderState: MutableState<CPSMenuBuilder?>,
    private val bottomBarBuilderState: MutableState<AdditionalBottomBarBuilder?>
) {
    val currentScreen by currentScreenState

    fun flowOfCurrentScreen(): Flow<Screen?> = navController.flowOfCurrentScreen()

    val isBottomBarEnabled: Boolean
        get() = currentScreen.let {
            it == null || it !is SettingsScreen
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
                subtitleState.value =
                    words.joinToString(prefix = "::", separator = ".", transform = String::lowercase)
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
            modifier = modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(500)) },
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

    val additionalBottomBar get() = bottomBarBuilderState.value
}