package com.example.test3.account_view

import androidx.lifecycle.*
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.BlockedState
import com.example.test3.utils.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel() : ViewModel() {

    fun reload(manager: AccountManager){
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if(savedInfo.isEmpty()) return@launch

            val blockedState = accountSmallViewBlockedState(manager.managerName)
            blockedState.value = BlockedState.BLOCKED

            val loadingState = accountLoadingState(manager.managerName)
            loadingState.value = LoadingState.LOADING

            val info = manager.loadInfo(savedInfo.userID, 1)

            if(info.status == STATUS.FAILED) {
                loadingState.value = LoadingState.FAILED
            } else {
                loadingState.value = LoadingState.PENDING
                if(info!=savedInfo) manager.setSavedInfo(info)
            }

            blockedState.value = BlockedState.UNBLOCKED
        }
    }

    private val loadingStates = mutableMapOf<String,MutableStateFlow<LoadingState>>()
    private fun accountLoadingState(managerName: String) = loadingStates.getOrPut(managerName) { MutableStateFlow(LoadingState.PENDING) }
    fun getAccountLoadingStateFlow(managerName: String) = accountLoadingState(managerName).asStateFlow()

    private val blockedStates = mutableMapOf<String,MutableStateFlow<BlockedState>>()
    private fun accountSmallViewBlockedState(managerName: String) = blockedStates.getOrPut(managerName) { MutableStateFlow(BlockedState.UNBLOCKED) }
    fun getAccountSmallViewBlockedState(managerName: String) = accountSmallViewBlockedState(managerName).asStateFlow()
}

