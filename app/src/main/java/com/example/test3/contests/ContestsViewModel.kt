package com.example.test3.contests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesContest
import com.example.test3.utils.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContestsViewModel: ViewModel() {

    private val loadingState = MutableStateFlow(LoadingState.PENDING)
    fun flowOfLoadingState(): StateFlow<LoadingState> = loadingState.asStateFlow()

    private val codeforcesContests = MutableStateFlow<List<CodeforcesContest>>(emptyList())
    fun flowOfCodeforcesContests(): StateFlow<List<CodeforcesContest>> = codeforcesContests.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            loadingState.value = LoadingState.LOADING
            val response = CodeforcesAPI.getContests() ?: return@launch
            val contests = response.result ?: return@launch
            codeforcesContests.value = contests
            loadingState.value = LoadingState.PENDING
        }
    }
}