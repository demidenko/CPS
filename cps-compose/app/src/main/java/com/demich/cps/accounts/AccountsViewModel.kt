package com.demich.cps.accounts

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.accounts.managers.UserInfo
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.launch


class AccountsViewModel: ViewModel() {
    private val loadingStatuses: MutableMap<AccountManagers, MutableState<LoadingStatus>> = mutableMapOf()

    fun loadingStatusFor(manager: AccountManager<*>): MutableState<LoadingStatus> =
        loadingStatuses.getOrPut(manager.type) { mutableStateOf(LoadingStatus.PENDING) }


    fun<U: UserInfo> reload(manager: AccountManager<U>) {
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if (savedInfo.isEmpty()) return@launch

            var loadingStatus by loadingStatusFor(manager)
            if (loadingStatus == LoadingStatus.LOADING) return@launch

            loadingStatus = LoadingStatus.LOADING
            val info = manager.loadInfo(savedInfo.userId, 1)

            if (info.status == STATUS.FAILED) {
                loadingStatus = LoadingStatus.FAILED
            } else {
                loadingStatus = LoadingStatus.PENDING
                manager.setSavedInfo(info)
            }
        }
    }

    fun<U: UserInfo> delete(manager: AccountManager<U>) {
        viewModelScope.launch {
            var loadingStatus by loadingStatusFor(manager)
            require(loadingStatus != LoadingStatus.LOADING)
            loadingStatus = LoadingStatus.PENDING
            manager.setSavedInfo(manager.emptyInfo())
        }
    }
}