package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.CPSDataStoreItem

class AccountDataStore<U: UserInfo>(
    dataStore: DataStore<Preferences>,
    val userInfo: CPSDataStoreItem<U>
): CPSDataStore(dataStore)

inline fun<reified U: UserInfo> AccountManager<U>.accountDataStore(
    dataStore: DataStore<Preferences>
) = AccountDataStore(
    dataStore = dataStore,
    userInfo = CPSDataStore(dataStore).itemJsonConvertible(
        name = "user_info",
        defaultValue = emptyInfo()
    )
)

open class AccountSettingsDataStore(dataStore: DataStore<Preferences>): CPSDataStore(dataStore) {
    protected open val keysForReset: List<DataStoreItem<*, *>> = emptyList()
    suspend fun resetRelatedItems() {
        val keys = keysForReset.takeIf { it.isNotEmpty() } ?: return
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