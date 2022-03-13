package com.demich.cps

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.demich.cps.accounts.CodeforcesAccountManager
import com.demich.cps.accounts.CodeforcesUserInfo
import com.demich.cps.accounts.STATUS
import com.demich.cps.accounts.makeUserInfoSpan
import com.demich.cps.ui.CPSDialog
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.context

@Composable
fun DevelopScreen(navController: NavController) {

    var startShow by rememberSaveable { mutableStateOf(true) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    val context = context
    val codeforcesAccountManager = CodeforcesAccountManager(context)

    Column {
        Button(onClick = { startShow = !startShow }) {
            Text("start title: $startShow")
        }
        Button(onClick = { showDialog = true }) {
            Text("show")
        }
        buildList {
            codeforcesAccountManager.ratingsUpperBounds.forEach { (rating, color) ->
                add(CodeforcesUserInfo(STATUS.OK, color.name, rating-1))
            }
            add(CodeforcesUserInfo(STATUS.OK, "RED", 2600))
            add(CodeforcesUserInfo(STATUS.OK, "NUTELLA", 3600))
            add(CodeforcesUserInfo(STATUS.OK, "NOT_RATED"))
            add(CodeforcesUserInfo(STATUS.NOT_FOUND, "NOT_FOUND"))
            add(CodeforcesUserInfo(STATUS.FAILED, "FAILED"))
        }.forEach { userInfo ->
            Text(makeUserInfoSpan(userInfo = userInfo, manager = codeforcesAccountManager))
        }
    }

    if (showDialog)
    CPSDialog(onDismissRequest = { showDialog = false }) {
        var showTitle by rememberSaveable { mutableStateOf(startShow) }
        Button(onClick = { showTitle = !showTitle }) {
            if (showTitle) Text("hide title")
            else Text("show title")
        }
        if (showTitle) {
            Text(text = "title", fontSize = 36.sp)
        }
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