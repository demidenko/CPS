package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.edit

abstract class AccountDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ItemizedDataStore(dataStoreWrapper) {
    abstract val userInfo: DataStoreItem<U?>
    abstract suspend fun onResetUserInfo()
}

abstract class AccountUniqueDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): AccountDataStore<U>(dataStoreWrapper) {
    final override suspend fun onResetUserInfo() {
        edit { prefs ->
            prefs[userInfo].let {
                prefs.clear()
                prefs[userInfo] = it
            }
        }
    }
}

inline fun<reified U: UserInfo> AccountManager<U>.accountDataStore(
    dataStoreWrapper: DataStoreWrapper
): AccountDataStore<U> =
    object : AccountDataStore<U>(dataStoreWrapper) {
        override val userInfo = jsonCPS.item<U?>(
            name = "user_info",
            defaultValue = null
        )

        override suspend fun onResetUserInfo() = Unit
    }


interface AccountSettingsProvider {
    fun getSettings(): ItemizedDataStore

    @Composable
    fun SettingsItems() {}
}