package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreWrapper
import com.demich.datastore_itemized.ItemizedDataStore

abstract class AccountDataStore<U: UserInfo>(
    dataStoreWrapper: DataStoreWrapper
): ItemizedDataStore(dataStoreWrapper) {
    abstract val userInfo: DataStoreItem<U?>
}

inline fun<reified U: UserInfo> AccountManager<U>.accountDataStore(
    dataStoreWrapper: DataStoreWrapper
): AccountDataStore<U> = object : AccountDataStore<U>(dataStoreWrapper) {
        override val userInfo = jsonCPS.item<U?>(
            name = "user_info",
            defaultValue = null
        )
    }


open class AccountSettingsDataStore(dataStoreWrapper: DataStoreWrapper): ItemizedDataStore(dataStoreWrapper) {
    protected open fun itemsForReset(): List<DataStoreItem<*>> = emptyList()
    suspend fun resetRelatedItems() = resetItems(itemsForReset())
}

interface AccountSettingsProvider {
    fun getSettings(): AccountSettingsDataStore

    @Composable
    fun SettingsItems() {}
}