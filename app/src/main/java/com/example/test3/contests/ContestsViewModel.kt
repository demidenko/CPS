package com.example.test3.contests

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.room.getContestsListDao
import com.example.test3.utils.CListAPI
import com.example.test3.utils.ClistContest
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

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch {
            val enabledPlatforms = context.settingsContests.enabledPlatforms()
            with(getContestsListDao(context)) {
                Contest.Platform.getAll().forEach { platform ->
                    if(platform !in enabledPlatforms) remove(platform)
                }
            }
            context.settingsContests.lastReloadedPlatforms(enabledPlatforms)
            reload(enabledPlatforms, context)
        }
    }

    fun reload(platforms: Collection<Contest.Platform>, context: Context) {
        if(platforms.isEmpty()) return
        viewModelScope.launch {
            loadingState.value = LoadingState.LOADING
            loadingState.value = run {
                val (login, apikey) = context.settingsContests.getClistApiLoginAndKey() ?: return@run LoadingState.FAILED
                val clistContests = CListAPI.getContests(
                    login, apikey, platforms,
                    getCurrentTimeSeconds() - TimeUnit.DAYS.toSeconds(7)
                ) ?: return@run LoadingState.FAILED
                val grouped = mapAndFilterClistResult(clistContests).groupBy { it.platform }
                with(getContestsListDao(context)) {
                    platforms.forEach { platform -> replace(platform, grouped[platform] ?: emptyList()) }
                }
                LoadingState.PENDING
            }
        }
    }

    private fun mapAndFilterClistResult(contests: Collection<ClistContest>): List<Contest> {
        return contests.mapNotNull {
            val contest = Contest(it)
            when {
                contest.platform == Contest.Platform.atcoder && it.host != "atcoder.jp" -> null
                else -> contest
            }
        }
    }
}