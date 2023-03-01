package com.demich.datastore_itemized

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ItemizedPreferences(private val preferences: Preferences) {
    operator fun<T> get(item: DataStoreItem<T>): T =
        (item as DataStoreBaseItem<T, *>).getValueFrom(preferences)
}

fun<D: ItemizedDataStore, R> D.flowBy(transform: suspend D.(ItemizedPreferences) -> R): Flow<R> =
    dataStore.data.map { transform(ItemizedPreferences(it)) }