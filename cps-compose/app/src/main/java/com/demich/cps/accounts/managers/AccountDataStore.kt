package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.ItemizedDataStore

abstract class AccountDataStore<U: UserInfo>(
    dataStore: DataStore<Preferences>
): ItemizedDataStore(dataStore) {
    abstract val userInfo: DataStoreItem<U>
}

inline fun<reified U: UserInfo> AccountManager<U>.accountDataStore(
    dataStore: DataStore<Preferences>
): AccountDataStore<U> = object : AccountDataStore<U>(dataStore = dataStore) {
        override val userInfo = jsonCPS.item(
            name = "user_info",
            defaultValue = emptyInfo()
        )
    }


open class AccountSettingsDataStore(dataStore: DataStore<Preferences>): ItemizedDataStore(dataStore) {
    protected open fun itemsForReset(): List<DataStoreItem<*>> = emptyList()
    suspend fun resetRelatedItems() = resetItems(itemsForReset())
}

interface AccountSettingsProvider {
    fun getSettings(): AccountSettingsDataStore

    @Composable
    fun Settings() {}
}