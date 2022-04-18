package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.utils.CListAPI
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

class ContestsViewModel: ViewModel() {
    var loadingStatus by mutableStateOf(LoadingStatus.PENDING)
        private set

    fun reload(context: Context) {
        viewModelScope.launch {
            loadingStatus = LoadingStatus.LOADING
            runCatching {
                val res = CListAPI.getContests(
                    context = context,
                    platforms = Contest.Platform.getAll(),
                    startTime = getCurrentTime() - 7.days
                )
                println(res)
            }.onSuccess {
                loadingStatus = LoadingStatus.PENDING
            }.onFailure {
                loadingStatus = LoadingStatus.FAILED
            }
        }
    }
}