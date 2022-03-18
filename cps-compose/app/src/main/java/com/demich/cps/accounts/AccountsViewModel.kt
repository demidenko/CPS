package com.demich.cps.accounts

import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.accounts.managers.UserInfo
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


class AccountsViewModel: ViewModel() {
    private val loadingStatuses: MutableMap<String, MutableState<LoadingStatus>> = mutableMapOf()

    fun loadingStatusFor(manager: AccountManager<*>): MutableState<LoadingStatus> =
        loadingStatuses.getOrPut(manager.managerName) { mutableStateOf(LoadingStatus.PENDING) }


    fun<U: UserInfo> reload(manager: AccountManager<U>) {
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if (savedInfo.isEmpty()) return@launch

            //val blockedState = accountSmallViewBlockedState(manager.managerName)
            //blockedState.value = BlockedState.BLOCKED

            var loadingStatus by loadingStatusFor(manager)
            //loadingStatus = if (loadingStatus == LoadingStatus.LOADING) LoadingStatus.PENDING else LoadingStatus.LOADING
            //return@launch

            if (loadingStatus == LoadingStatus.LOADING) return@launch
            loadingStatus = LoadingStatus.LOADING


            delay(Random.nextLong(5000, 15000))
            if (Random.nextBoolean()) {
                loadingStatus = LoadingStatus.FAILED
                return@launch
            }

            val info = manager.loadInfo(savedInfo.userId, 1)

            if (info.status == STATUS.FAILED) {
                loadingStatus = LoadingStatus.FAILED
            } else {
                loadingStatus = LoadingStatus.PENDING
                manager.setSavedInfo(info)
            }

            //blockedState.value = BlockedState.UNBLOCKED
        }
    }
}