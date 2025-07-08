package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreItem<T>
internal constructor(
    dataStore: DataStore<Preferences>,
    internal val saver: PreferencesSaver<T>
): DataStoreValue<T>(dataStore = dataStore, reader = saver) {

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

abstract class DataStoreValue<out T>
internal constructor(
    protected val dataStore: DataStore<Preferences>,
    internal val reader: PreferencesReader<T>
) {
    constructor(dataStoreValue: DataStoreValue<T>): this(
        dataStore = dataStoreValue.dataStore,
        reader = dataStoreValue.reader
    )

    suspend operator fun invoke(): T =
        reader.restore(dataStore.data.first())

    fun asFlow(): Flow<T> =
        dataStore.data
            .distinctUntilChanged(reader::prefsEquivalent)
            .map(reader::restore)
}