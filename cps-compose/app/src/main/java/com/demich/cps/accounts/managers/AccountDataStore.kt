package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.CPSDataStoreItem

abstract class AccountDataStore<U: UserInfo>(
    dataStore: DataStore<Preferences>
): CPSDataStore(dataStore) {
    abstract val userInfo: CPSDataStoreItem<U>
}

inline fun<reified U: UserInfo> AccountManager<U>.accountDataStore(
    dataStore: DataStore<Preferences>
): AccountDataStore<U> = object : AccountDataStore<U>(dataStore = dataStore) {
        override val userInfo = itemJsonable(
            name = "user_info",
            defaultValue = emptyInfo()
        )
    }


open class AccountSettingsDataStore(dataStore: DataStore<Preferences>): CPSDataStore(dataStore) {
    protected open fun keysForReset(): List<CPSDataStoreItem<*>> = emptyList()
    suspend fun resetRelatedItems() {
        val keys = keysForReset().takeIf { it.isNotEmpty() } ?: return
        dataStore.edit { prefs ->
            keys.forEach { prefs.remove(it.key) }
        }
    }
}

interface AccountSettingsProvider {
    fun getSettings(): AccountSettingsDataStore

    @Composable
    fun Settings() {}
}