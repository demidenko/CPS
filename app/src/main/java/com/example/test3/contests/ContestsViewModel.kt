package com.example.test3.contests

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.room.getContestsListDao
import com.example.test3.utils.CListAPI
import com.example.test3.utils.LoadingState
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ContestsViewModel: ViewModel() {

    private val loadingState = MutableStateFlow(LoadingState.PENDING)
    fun flowOfLoadingState(): StateFlow<LoadingState> = loadingState.asStateFlow()

    fun reload(context: Context) {
        viewModelScope.launch {
            loadingState.value = LoadingState.LOADING
            loadingState.value = run {
                val (login, apikey) = context.settingsContests.getClistApiLoginAndKey() ?: return@run LoadingState.FAILED
                val platforms = listOf(Contest.Platform.codeforces, Contest.Platform.atcoder, Contest.Platform.topcoder)
                val contestsList = CListAPI.getContests(
                    login, apikey, platforms,
                    getCurrentTimeSeconds() - TimeUnit.DAYS.toSeconds(7)
                ) ?: return@run LoadingState.FAILED
                val grouped = contestsList.map { Contest(it) }.groupBy { it.platform }
                val dao = getContestsListDao(context)
                platforms.forEach { platform -> dao.replace(platform, grouped[platform] ?: emptyList()) }
                LoadingState.PENDING
            }
        }
    }
}