package com.example.test3.contests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContestsViewModel: ViewModel() {

    private val loadingState = MutableStateFlow(LoadingState.PENDING)
    fun flowOfLoadingState(): StateFlow<LoadingState> = loadingState.asStateFlow()

    private val contestsStateFlow = MutableStateFlow<List<Contest>>(emptyList())
    fun flowOfContests(): StateFlow<List<Contest>> = contestsStateFlow.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            loadingState.value = LoadingState.LOADING
            val response = CodeforcesAPI.getContests() ?: return@launch
            val contests = response.result ?: return@launch
            contestsStateFlow.value = contests.map { Contest(it) }
            loadingState.value = LoadingState.PENDING
        }
    }
}