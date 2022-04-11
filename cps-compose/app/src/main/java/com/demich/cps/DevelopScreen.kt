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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import com.demich.cps.accounts.makeUserInfoSpan
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.ui.RatingGraph
import com.demich.cps.ui.RatingGraphUIStates
import com.demich.cps.ui.rememberRatingGraphUIStates
import com.demich.cps.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevelopScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    val context = context
    val codeforcesAccountManager = CodeforcesAccountManager(context)

    Column {
        val initial = remember {
            buildList {
                codeforcesAccountManager.ratingsUpperBounds.forEach { (color, rating) ->
                    add(CodeforcesUserInfo(STATUS.OK, color.name, rating - 1))
                }
                add(CodeforcesUserInfo(STATUS.OK, "RED", 2600))
                add(CodeforcesUserInfo(STATUS.OK, "NUTELLA", 3600))
                add(CodeforcesUserInfo(STATUS.OK, "Not rated"))
                add(CodeforcesUserInfo(STATUS.NOT_FOUND, "Not found"))
                add(CodeforcesUserInfo(STATUS.FAILED, "Failed"))
            }
        }


        Row {
            var list by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(initial) }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = list, key = { it.handle }) {
                    Text(
                        text = makeUserInfoSpan(userInfo = it, manager = codeforcesAccountManager),
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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


        val ratingGraphUIStates = rememberRatingGraphUIStates().apply {
            showRatingGraphState.value = true
            loadingStatusState.value = LoadingStatus.PENDING
        }

        RatingGraph(
            ratingGraphUIStates = ratingGraphUIStates,
            manager = CodeforcesAccountManager(context)
        )

        Button(
            onClick = {
                val n = (1 shl Random.nextInt(5)) - 1
                val list = mutableListOf<RatingChange>()
                repeat(n) {
                    val date = (getCurrentTime() - 4000.days) + Random.nextLong(
                        from = 0,
                        until = 4000.days.inWholeSeconds
                    ).seconds
                    val rating = Random.nextInt(from = 0, until = 3000)
                    list.add(RatingChange(rating, date))
                }
                ratingGraphUIStates.ratingChangesState.value = list.sortedBy { it.date }
            }
        ) {
            Text("random graph")
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


