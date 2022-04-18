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
import kotlin.time.Duration.Companion.days

class ContestsViewModel: ViewModel() {
    var loadingStatus by mutableStateOf(LoadingStatus.PENDING)
        private set

    private val contestsStateFlow = MutableStateFlow<List<Contest>>(emptyList())
    fun flowOfContests() = contestsStateFlow.asStateFlow()

    fun reload(context: Context) {
        viewModelScope.launch {
            loadingStatus = LoadingStatus.LOADING
            runCatching {
                val contests = CListAPI.getContests(
                    context = context,
                    platforms = Contest.Platform.getAll(),
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