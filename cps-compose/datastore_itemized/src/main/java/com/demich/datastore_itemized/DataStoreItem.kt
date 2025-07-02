package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreItem<T>
internal constructor(
    private val dataStore: DataStore<Preferences>,
    internal val saver: PreferencesSaver<T>
) {
    fun asFlow(): Flow<T> =
        dataStore.data
            .distinctUntilChanged(saver::prefsEquivalent)
            .map(saver::restore)

    //getter
    suspend operator fun invoke(): T = saver.restore(dataStore.data.first())

    suspend fun setValue(value: T) {
        dataStore.edit { prefs ->
            saver.save(prefs, value)
        }
    }

    suspend fun update(transform: (T) -> T) {
        dataStore.edit { prefs ->
            saver.save(prefs, transform(saver.restore(prefs)))
        }
    }
}

internal interface PreferencesSaver<T> {
    fun save(prefs: MutablePreferences, value: T)

    fun restore(prefs: Preferences): T

    fun removeFrom(prefs: MutablePreferences)

    fun prefsEquivalent(old: Preferences, new: Preferences): Boolean
}