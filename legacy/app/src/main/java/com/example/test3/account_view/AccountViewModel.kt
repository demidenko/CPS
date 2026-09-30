package com.example.test3.account_view

import android.content.Context
import androidx.lifecycle.*
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.CListAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.utils.BlockedState
import com.example.test3.utils.CListUtils
import com.example.test3.utils.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class AccountViewModel() : ViewModel() {

    fun<U: UserInfo> reload(manager: AccountManager<U>){
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
    fun getAccountSmallViewBlockedState(manager: AccountManager<*>) = accountSmallViewBlockedState(manager.managerName).asStateFlow()


    private val clistImportProgress = MutableStateFlow<Pair<Int,Int>?>(null)
    fun getClistImportProgress() = clistImportProgress.asStateFlow()
    fun clistImport(clistUserInfo: CListAccountManager.CListUserInfo, context: Context) {
        viewModelScope.launch {
            val supported = clistUserInfo.accounts.mapNotNull { (resource, userData) ->
                CListUtils.getManager(resource, userData.first, userData.second, context)
            }

            clistImportProgress.value = 0 to supported.size
            var done = 0

            supported.map { (manager, userID) ->
                launch {
                    val blockedState = accountSmallViewBlockedState(manager.managerName)
                    blockedState.first { it == BlockedState.UNBLOCKED }
                    blockedState.value = BlockedState.BLOCKED
                    manager.loadAndSave(userID)
                    blockedState.value = BlockedState.UNBLOCKED
                    clistImportProgress.value = ++done to supported.size
                }
            }.joinAll()

            clistImportProgress.value = null

        }
    }

    private suspend fun<U: UserInfo> AccountManager<U>.loadAndSave(userID: String) {
        setSavedInfo(loadInfo(userID))
    }
}

