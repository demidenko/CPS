package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


abstract class ItemizedDataStore(wrapper: DataStoreWrapper) {
    private val dataStore: DataStore<Preferences> = wrapper.dataStore

    protected suspend fun resetItems(items: Collection<DataStoreItem<*>>) {
        if (items.isEmpty()) return
        dataStore.edit { prefs ->
            items.forEach { prefs.remove((it as DataStoreBaseItem<*,*>).key) }
        }
    }

    private fun<T: Any> item(key: Preferences.Key<T>, defaultValue: T): DataStoreItem<T> =
        Item(key = key, defaultValue = defaultValue, dataStore = dataStore)

    private fun<T: Any> itemNullable(key: Preferences.Key<T>): DataStoreItem<T?> =
        ItemNullable(key = key, dataStore = dataStore)

    protected fun<T> itemStringConvertible(name: String, defaultValue: T, encode: (T) -> String, decode: (String) -> T): DataStoreItem<T> =
        ItemConvertible(key = stringPreferencesKey(name), defaultValue = defaultValue, encode = encode, decode = decode, dataStore = dataStore)

    protected fun<T> itemStringSetConvertible(name: String, defaultValue: T, encode: (T) -> Set<String>, decode: (Set<String>) -> T): DataStoreItem<T> =
        ItemConvertible(key = stringSetPreferencesKey(name), defaultValue = defaultValue, encode = encode, decode = decode, dataStore = dataStore)



    protected fun itemBoolean(name: String, defaultValue: Boolean): DataStoreItem<Boolean> =
        item(booleanPreferencesKey(name), defaultValue)

    protected fun itemInt(name: String, defaultValue: Int): DataStoreItem<Int> =
        item(intPreferencesKey(name), defaultValue)

    protected fun itemIntNullable(name: String): DataStoreItem<Int?> =
        itemNullable(intPreferencesKey(name))

    protected fun itemLong(name: String, defaultValue: Long): DataStoreItem<Long> =
        item(longPreferencesKey(name), defaultValue)

    protected fun itemLongNullable(name: String): DataStoreItem<Long?> =
        itemNullable(longPreferencesKey(name))

    protected fun itemString(name: String, defaultValue: String): DataStoreItem<String> =
        item(stringPreferencesKey(name), defaultValue)

    protected fun itemStringNullable(name: String): DataStoreItem<String?> =
        itemNullable(stringPreferencesKey(name))

    protected inline fun<reified T: Enum<T>> itemEnum(name: String, defaultValue: T): DataStoreItem<T> =
        itemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = Enum<T>::name,
            decode = ::enumValueOf
        )

    protected inline fun<reified T: Enum<T>> itemEnumSet(name: String, defaultValue: Set<T> = emptySet()): DataStoreItem<Set<T>> =
        itemStringSetConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = { it.mapTo(mutableSetOf(), Enum<T>::name) },
            decode = { it.mapTo(mutableSetOf(), ::enumValueOf) }
        )

    protected inline fun<reified T> Json.item(name: String, defaultValue: T): DataStoreItem<T> =
        itemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = ::encodeToString,
            decode = ::decodeFromString
        )
}
