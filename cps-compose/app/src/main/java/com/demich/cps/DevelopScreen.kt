package com.demich.cps

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.demich.cps.ui.CounterButton
import com.demich.cps.utils.CPSDataStore

@Composable
fun DevelopScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CounterButton(text = "dev")
    }
}


val Context.settingsDev: SettingsDev
    get() = SettingsDev(this)

class SettingsDev(context: Context) : CPSDataStore(context.settings_dev_dataStore) {
    companion object {
        private val Context.settings_dev_dataStore by preferencesDataStore("settings_develop")
    }

    val devModeEnabled = Item(booleanPreferencesKey("develop_enabled"), false)

}