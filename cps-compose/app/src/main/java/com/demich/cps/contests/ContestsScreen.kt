package com.demich.cps.contests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.demich.cps.Screen
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.ui.CounterButton
import com.demich.cps.utils.LoadingStatus

@Composable
fun ContestsScreen(navController: NavController) {

}

@Composable
fun ContestsBottomBar(navController: NavController) {
    CPSIconButton(icon = Icons.Default.Settings) {
        navController.navigate(Screen.ContestsSettings.route)
    }
    CPSReloadingButton(loadingStatus = LoadingStatus.PENDING) {

    }
}

@Composable
fun ContestsSettingsScreen(navController: NavController) {
    CounterButton(text = "cs")
}
