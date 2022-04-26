package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val contestsStateFlow = MutableStateFlow<List<Contest>>(emptyList())
    fun flowOfContests() = contestsStateFlow.asStateFlow()

    private val errorStateFlow = MutableStateFlow<Throwable?>(null)
    fun flowOfError() = errorStateFlow.asStateFlow()

    fun reload(context: Context) {
        viewModelScope.launch {
            loadingStatus = LoadingStatus.LOADING
            errorStateFlow.value = null
            val settings = context.settingsContests
            runCatching {
                CListApi.getContests(
                    apiAccess = settings.clistApiAccess(),
                    platforms = settings.enabledPlatforms(),
                    startTime = getCurrentTime() - 7.days
                ).mapAndFilterResult()
            }.onSuccess { contests ->
                loadingStatus = LoadingStatus.PENDING
                contestsStateFlow.value = contests
            }.onFailure {
                loadingStatus = LoadingStatus.FAILED
                errorStateFlow.value = it
            }
        }
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