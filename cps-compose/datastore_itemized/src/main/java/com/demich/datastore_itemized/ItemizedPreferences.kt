package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ItemizedPreferences(private val preferences: Preferences) {
    operator fun<T> get(item: DataStoreItem<T>): T =
        item.converter.getFrom(preferences)
}

class ItemizedMutablePreferences(private val preferences: MutablePreferences) {
    operator fun<T> get(item: DataStoreItem<T>): T =
        item.converter.getFrom(preferences)

    operator fun<T> set(item: DataStoreItem<T>, value: T) =
        item.converter.setTo(preferences, value)

    fun<T> remove(item: DataStoreItem<T>) {
        preferences.remove(item.converter.key)
    }
}

fun<D: ItemizedDataStore, R> D.flowBy(transform: D.(ItemizedPreferences) -> R): Flow<R> =
    dataStore.data.map { transform(ItemizedPreferences(it)) }

suspend fun<D: ItemizedDataStore> D.edit(block: D.(ItemizedMutablePreferences) -> Unit) {
    dataStore.edit { block(ItemizedMutablePreferences(it)) }
}