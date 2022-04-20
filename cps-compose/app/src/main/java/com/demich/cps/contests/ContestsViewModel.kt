package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.utils.CListAPI
import com.demich.cps.utils.ClistContest
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class ContestsViewModel: ViewModel() {
    var loadingStatus by mutableStateOf(LoadingStatus.PENDING)
        private set

    private val contestsStateFlow = MutableStateFlow<List<Contest>>(emptyList())
    fun flowOfContests() = contestsStateFlow.asStateFlow()

    fun reload(context: Context) {
        viewModelScope.launch {
            loadingStatus = LoadingStatus.LOADING
            val settings = context.settingsContests
            runCatching {
                val contests = CListAPI.getContests(
                    apiAccess = settings.clistApiAccess(),
                    platforms = settings.enabledPlatforms(),
                    startTime = getCurrentTime() - 7.days
                ).mapAndFilterResult()
                contestsStateFlow.value = contests
            }.onSuccess {
                loadingStatus = LoadingStatus.PENDING
            }.onFailure {
                loadingStatus = LoadingStatus.FAILED
            }
        }
    }

    fun addRandomContest() {
        val currentTime = getCurrentTime()
        val duration = Random.nextLong(from = -30, until = 30).seconds
        val contest = Contest(
            title = Random.nextBits(10).toString(2),
            startTime = currentTime + duration,
            durationSeconds = 60,
            platform = null,
            id = Random.nextLong().toString()
        )
        contestsStateFlow.value += contest
    }
}

private fun Collection<ClistContest>.mapAndFilterResult(): List<Contest> {
    return mapNotNull {
        val contest = Contest(it)
        when {
            contest.platform == Contest.Platform.atcoder && it.host != "atcoder.jp" -> null
            else -> contest
        }
    }
}