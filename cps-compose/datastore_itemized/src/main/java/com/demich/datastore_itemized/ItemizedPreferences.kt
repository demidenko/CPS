package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class ItemizedPreferences internal constructor(
    protected open val preferences: Preferences
) {
    operator fun<T> get(item: DataStoreItem<T>): T =
        item.converter.getFrom(preferences)
}

class ItemizedMutablePreferences internal constructor(
    override val preferences: MutablePreferences
): ItemizedPreferences(preferences) {
    operator fun<T> set(item: DataStoreItem<T>, value: T) =
        item.converter.setTo(preferences, value)

    fun clear() {
        preferences.clear()
    }

    fun<T> remove(item: DataStoreItem<T>) {
        item.converter.removeFrom(preferences)
    }

    //maps editing
    private inner class ConverterMap<K, V>(private val item: DataStoreItem<Map<K, V>>) {
        val map: MutableMap<K, V> = get<Map<K, V>>(item).toMutableMap()
        fun save() { set(item, map) }
    }

    private val mutableMaps = mutableMapOf<String, ConverterMap<*, *>>()

    @Suppress("UNCHECKED_CAST")
    operator fun<K, V> get(item: DataStoreItem<Map<K, V>>): MutableMap<K, V> =
        mutableMaps.getOrPut(item.converter.name) { ConverterMap(item) }.map as MutableMap<K, V>

    internal fun saveMutableMaps() {
        mutableMaps.values.forEach { it.save() }
    }
}

fun <D: ItemizedDataStore, R> D.flowOf(transform: D.(ItemizedPreferences) -> R): Flow<R> =
    dataStore.data.map { transform(ItemizedPreferences(it)) }

suspend fun <D: ItemizedDataStore> D.edit(block: D.(ItemizedMutablePreferences) -> Unit) {
    dataStore.edit {
        val prefs = ItemizedMutablePreferences(it)
        block(prefs)
        prefs.saveMutableMaps()
    }
}

@OptIn(ExperimentalContracts::class)
suspend inline fun <D: ItemizedDataStore, R> D.fromSnapshot(block: D.(ItemizedPreferences) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(snapshot())
}