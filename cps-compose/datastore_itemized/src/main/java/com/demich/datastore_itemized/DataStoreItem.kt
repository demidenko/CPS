package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DataStoreItem<T>
internal constructor(
    private val dataStore: DataStore<Preferences>,
    internal val converter: Converter<T, *>
) {
    fun asFlow(): Flow<T> = converter.flowFrom(dataStore.data)

    //getter
    suspend operator fun invoke(): T = converter.restore(dataStore.data.first())

    suspend fun setValue(value: T) {
        dataStore.edit { prefs ->
            converter.save(prefs, value)
        }
    }

    suspend fun update(transform: (T) -> T) {
        dataStore.edit { prefs ->
            converter.save(prefs, transform(converter.restore(prefs)))
        }
    }
}

internal interface PreferencesSaver<T> {
    fun save(prefs: MutablePreferences, value: T)

    fun restore(prefs: Preferences): T
}