package com.demich.cps.news

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.demich.cps.ui.CPSIconButton

@Composable
fun NewsScreen(navController: NavController) {
    val listState = rememberLazyListState()
    LazyColumn(state = listState) {
        items(count = 1e9.toInt(), key = { it }) { index ->
            Text("$index = ${index.toString(2)}")
        }
    }
}

@Composable
fun NewsSettingsScreen() {

}

@Composable
fun NewsBottomBar() {
    CPSIconButton(icon = Icons.Default.Refresh) {

    }
}

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String {
        return when(this){
            EN -> "en"
            RU -> "ru"
        }
    }
}