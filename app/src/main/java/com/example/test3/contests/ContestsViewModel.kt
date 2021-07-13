package com.example.test3.contests

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private var touched = false
    private val contestsStateFlow = MutableStateFlow<List<Contest>>(emptyList())
    fun flowOfContests(context: Context): StateFlow<List<Contest>> =
        contestsStateFlow.also {
            if(!touched) {
                touched = true
                reload(context)
            }
        }.asStateFlow()

    fun reload(context: Context) {
        viewModelScope.launch {
            /*loadingState.value = LoadingState.LOADING
            loadingState.value = CodeforcesAPI.getContests()?.let { response ->
                response.result?.let { contests ->
                    contestsStateFlow.value = contests.map { Contest(it) }
                    LoadingState.PENDING
                }
            } ?: LoadingState.FAILED*/


            loadingState.value = LoadingState.LOADING

            loadingState.value = run {
                val login = context.settingsContests.getClistApiLogin() ?: return@run LoadingState.FAILED
                val apikey = context.settingsContests.getClistApiKey() ?: return@run LoadingState.FAILED
                val contests = CListAPI.getContests(
                    login,
                    apikey,
                    listOf(Contest.Platform.codeforces, Contest.Platform.atcoder, Contest.Platform.topcoder),
                    getCurrentTimeSeconds() - TimeUnit.DAYS.toSeconds(7)
                ) ?: return@run LoadingState.FAILED
                contestsStateFlow.value = contests.map { Contest(it) }
                LoadingState.PENDING
            }
        }
    }
}