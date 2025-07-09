package com.demich.cps.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.demich.cps.ui.CPSMenuBuilder
import com.demich.cps.ui.StartScreenDataStore
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.ratedProfilesColorState
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.ui.topbar.CPSTopBar
import com.demich.cps.utils.backgroundColor
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.getValue
import com.demich.cps.utils.rememberFrom
import com.demich.cps.utils.writeOnlyProperty
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
        get() = currentScreen !is Screen.NoBottomBarScreen

    fun navigateTo(screen: Screen) {
        val currentScreen = currentScreen ?: return
        if (screen.rootScreen != currentScreen.rootScreen) {
            //switch stack
            navController.navigate(route = screen) {
                popUpTo(currentScreen.rootScreen) {
                    saveState = true
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            if (screen != currentScreen) {
                navController.navigate(route = screen)
            }
        }
    }

    fun popBack() {
        navController.popBackStack()
    }

    inner class ScreenScope<T: Screen>(
        val screen: T
    ) {
        var menu: CPSMenuBuilder? by writeOnlyProperty {
            if (screen == currentScreen) menuBuilderState.value = it
        }

        var bottomBar: AdditionalBottomBarBuilder? by writeOnlyProperty {
            if (screen == currentScreen) bottomBarBuilderState.value = it
        }

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
        val startScreen: Screen = rememberFrom(context) {
            runBlocking { StartScreenDataStore(it).startRootScreen() }
        }
        androidx.navigation.compose.NavHost(
            navController = navController,
            startDestination = startScreen,
            modifier = modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(500)) },
            builder = builder
        )
    }

    @Composable
    fun TopBarWithStatusBar(modifier: Modifier = Modifier) {
        val statusBarColor by ratedProfilesColorState(
            navigator = this,
            disabledColor = cpsColors.background
        )

        Box(
            modifier
                .backgroundColor { statusBarColor }
                .statusBarsPadding()
        ) {
            CPSTopBar(
                subtitle = { subtitleState.value },
                additionalMenu = { menuBuilderState.value }
            )
        }
    }

    val additionalBottomBar: AdditionalBottomBarBuilder
        get() = bottomBarBuilderState.value ?: {}
}

context(builder: NavGraphBuilder)
inline fun <reified T: Screen> CPSNavigator.navEntry(
    includeFontPadding: Boolean = true,
    crossinline content: @Composable CPSNavigator.ScreenScope<T>.() -> Unit
) {
    builder.composable<T> {
        val scope = remember {
            ScreenScope(it.toRoute<T>()).apply {
                menu = null
                bottomBar = null
            }
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            //TODO: remove it (use default includeFontPadding = false)
            ProvideTextStyle(TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = includeFontPadding))) {
                scope.content()
            }
        }
    }
}

