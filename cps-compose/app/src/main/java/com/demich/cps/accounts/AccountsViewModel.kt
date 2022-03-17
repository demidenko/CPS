package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.accounts.managers.UserInfo
import com.demich.cps.utils.LoadingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


class AccountsViewModel: ViewModel() {
    private val loadingStates: MutableMap<String, MutableState<LoadingState>> = mutableMapOf()

    fun loadingStateFor(manager: AccountManager<*>): MutableState<LoadingState> =
        loadingStates.getOrPut(manager.managerName) { mutableStateOf(LoadingState.PENDING) }

    fun reloadAll(context: Context) {
        viewModelScope.launch {
            context.allAccountManagers
                .filterNot { it.getSavedInfo().isEmpty() }
                .forEach { manager -> reload(manager) }
        }
    }


    fun<U: UserInfo> reload(manager: AccountManager<U>) {
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if (savedInfo.isEmpty()) return@launch

            //val blockedState = accountSmallViewBlockedState(manager.managerName)
            //blockedState.value = BlockedState.BLOCKED

            var loadingState by loadingStateFor(manager)
            if (loadingState == LoadingState.LOADING) return@launch
            loadingState = LoadingState.LOADING


            delay(Random.nextLong(5000, 15000))
            val info = manager.loadInfo(savedInfo.userId, 1)

            if (info.status == STATUS.FAILED) {
                loadingState = LoadingState.FAILED
            } else {
                loadingState = LoadingState.PENDING
                manager.setSavedInfo(info)
            }

            //blockedState.value = BlockedState.UNBLOCKED
        }
    }
}