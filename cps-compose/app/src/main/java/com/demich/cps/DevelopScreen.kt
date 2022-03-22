package com.demich.cps

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.demich.cps.accounts.makeUserInfoSpan
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevelopScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    val context = context
    val codeforcesAccountManager = CodeforcesAccountManager(context)

    Column {
        val initial = buildList {
            codeforcesAccountManager.ratingsUpperBounds.forEach { (color, rating) ->
                add(CodeforcesUserInfo(STATUS.OK, color.name, rating-1))
            }
            add(CodeforcesUserInfo(STATUS.OK, "RED", 2600))
            add(CodeforcesUserInfo(STATUS.OK, "NUTELLA", 3600))
            add(CodeforcesUserInfo(STATUS.OK, "Not rated"))
            add(CodeforcesUserInfo(STATUS.NOT_FOUND, "Not found"))
            add(CodeforcesUserInfo(STATUS.FAILED, "Failed"))
        }


        var list by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(initial) }

        LazyColumn {
            items(items = list, key = { it.handle }) {
                Text(
                    text = makeUserInfoSpan(userInfo = it, manager = codeforcesAccountManager),
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }

        Row {
            Button(onClick = { list = list.shuffled() }) {
                Text("Shuffle")
            }
            Button(onClick = { list = initial.filter { Random.nextBoolean() } }) {
                Text("Random")
            }
            Button(onClick = { list = initial }) {
                Text("Reset")
            }
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



@Composable
fun ContentLoadingButton(
    text: String,
    coroutineScope: CoroutineScope,
    block: suspend () -> Unit
) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    Button(
        enabled = enabled,
        onClick = {
            enabled = false
            coroutineScope.launch {
                block()
                enabled = true
            }
        }
    ) {
        Text(text = text)
    }
}


