package com.example.test3.account_view

import androidx.lifecycle.*
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.LoadingState
import kotlinx.coroutines.launch

class AccountViewModel() : ViewModel() {

    fun reload(manager: AccountManager){
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if(savedInfo.isEmpty()) return@launch

            val loadingState = accountLoadingState(manager.managerName)
            loadingState.value = LoadingState.LOADING

            val info = manager.loadInfo(savedInfo.userID, 1)

            if(info.status == STATUS.FAILED) {
                loadingState.value = LoadingState.FAILED
            } else {
                loadingState.value = LoadingState.PENDING
                if(info!=savedInfo) manager.setSavedInfo(info)
            }
        }
    }

    private val states = mutableMapOf<String,MutableLiveData<LoadingState>>()

    private fun accountLoadingState(managerName: String) = states.getOrPut(managerName) { MutableLiveData(LoadingState.PENDING) }

    fun getAccountLoadingStateLiveData(managerName: String): LiveData<LoadingState> = accountLoadingState(managerName)
}

