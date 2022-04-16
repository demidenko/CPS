package com.demich.cps.accounts.managers

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.demich.cps.utils.CPSDataStore

class AccountDataStore<U: UserInfo>(
    dataStore: DataStore<Preferences>,
    val userInfo: ItemStringConvertible<U>
): CPSDataStore(dataStore)

inline fun <reified U: UserInfo> accountDataStore(dataStore: DataStore<Preferences>, emptyUserInfo: U): AccountDataStore<U> {
    return AccountDataStore(
        dataStore,
        CPSDataStore(dataStore).itemJsonConvertible(
            name = "user_info",
            defaultValue = emptyUserInfo
        )
    )
}

open class AccountSettingsDataStore(dataStore: DataStore<Preferences>): CPSDataStore(dataStore) {
    protected open val keysForReset: List<DataStoreItem<*, *>> = emptyList()
    suspend fun resetRelatedData() {
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