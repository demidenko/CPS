package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DataStoreItem<T>
internal constructor(
    private val dataStore: DataStore<Preferences>,
    internal val converter: Converter<T, *>
) {
    val flow: Flow<T>
        get() = converter.flowFrom(dataStore.data)

    //getter
    suspend operator fun invoke(): T = converter.getFrom(dataStore.data.first())

    //setter
    suspend operator fun invoke(newValue: T) {
        dataStore.edit { prefs ->
            converter.setTo(prefs, newValue)
        }
    }

    suspend fun update(transform: (T) -> T) {
        dataStore.edit { prefs ->
            converter.setTo(prefs, transform(converter.getFrom(prefs)))
        }
    }
}