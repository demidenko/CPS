package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.room.contestsListDao
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.ClistContest
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

class ContestsViewModel: ViewModel() {
    var loadingStatus by mutableStateOf(LoadingStatus.PENDING)
        private set

    private val errorStateFlow = MutableStateFlow<Throwable?>(null)
    fun flowOfError() = errorStateFlow.asStateFlow()

    fun reloadEnabledPlatforms(context: Context) {
        viewModelScope.launch {
            val settings = context.settingsContests
            val enabledPlatforms = settings.enabledPlatforms()
            context.contestsListDao.run {
                Contest.getPlatforms().forEach { platform ->
                    if (platform !in enabledPlatforms) remove(platform)
                }
            }
            reload(platforms = enabledPlatforms, context = context)
        }
    }

    private suspend fun reload(platforms: Collection<Contest.Platform>, context: Context) {
        if (platforms.isEmpty()) return

        errorStateFlow.value = null
        loadingStatus = LoadingStatus.LOADING

        runCatching {
            val settings = context.settingsContests
            CListApi.getContests(
                apiAccess = settings.clistApiAccess(),
                platforms = settings.enabledPlatforms(),
                startTime = getCurrentTime() - 7.days
            ).mapAndFilterResult()
        }.onSuccess { contests: List<Contest> ->
            val grouped = contests.groupBy { it.platform }
            context.contestsListDao.run {
                platforms.forEach { platform ->
                    replace(platform = platform, contests = grouped[platform] ?: emptyList())
                }
            }
            loadingStatus = LoadingStatus.PENDING
        }.onFailure {
            loadingStatus = LoadingStatus.FAILED
            errorStateFlow.value = it
        }
    }
}

private fun Collection<ClistContest>.mapAndFilterResult(): List<Contest> {
    return mapNotNull {
        val contest = Contest(it)
        when (contest.platform) {
            Contest.Platform.atcoder -> {
                if (it.host == "atcoder.jp")
                    contest.copy(title = contest.title.replace("（", " (").replace('）',')'))
                else null
            }
            else -> contest
        }
    }
}