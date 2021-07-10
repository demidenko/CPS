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

    private var touched = false
    private val contestsStateFlow = MutableStateFlow<List<Contest>>(emptyList())
    fun flowOfContests(): StateFlow<List<Contest>> =
        contestsStateFlow.also {
            if(!touched) {
                touched = true
                reload()
            }
        }.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            loadingState.value = LoadingState.LOADING
            loadingState.value = CodeforcesAPI.getContests()?.let { response ->
                response.result?.let { contests ->
                    contestsStateFlow.value = contests.map { Contest(it) }
                    LoadingState.PENDING
                }
            } ?: LoadingState.FAILED
        }
    }
}