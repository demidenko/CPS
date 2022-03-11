package com.demich.cps.contests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.demich.cps.Screen
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CounterButton

@Composable
fun ContestsScreen(navController: NavController) {

}

@Composable
fun ContestsBottomBar(navController: NavController) {
    CPSIconButton(icon = Icons.Default.Settings) {
        navController.navigate(Screen.ContestsSettings.route)
    }
    CPSIconButton(icon = Icons.Default.Refresh) {

    }
}

@Composable
fun ContestsSettingsScreen(navController: NavController) {
    CounterButton(text = "cs")
}
