package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.EnumSet


abstract class ItemizedDataStore(wrapper: DataStoreWrapper) {
    internal val dataStore: DataStore<Preferences> = wrapper.dataStore

    suspend fun snapshot(): ItemizedPreferences = ItemizedPreferences(dataStore.data.first())

    protected suspend fun resetAll() {
        dataStore.updateData { emptyPreferences() }
    }

    protected suspend fun resetItems(items: Collection<DataStoreItem<*>>) {
        if (items.isEmpty()) return
        dataStore.edit { prefs ->
            items.forEach { it.saver.removeFrom(prefs) }
        }
    }

    private fun <T> dataStoreItem(saver: PreferencesSaver<T>): DataStoreItem<T> =
        DataStoreItem(dataStore = dataStore, saver = saver)

    private fun <T: Any> item(key: Preferences.Key<T>, defaultValue: T): DataStoreItem<T> =
        dataStoreItem(saver = ValueWithDefault(key, defaultValue))

    private fun <T: Any> itemNullable(key: Preferences.Key<T>): DataStoreItem<T?> =
        dataStoreItem(saver = ValueNullable(key))


    protected fun <T> itemStringConvertible(
        name: String,
        defaultValue: () -> T,
        encode: (T) -> String,
        decode: (String) -> T
    ): DataStoreItem<T> =
        dataStoreItem(saver = ValueConvertible(stringPreferencesKey(name), defaultValue, encode, decode))

    protected fun <T> itemStringSetConvertible(
        name: String,
        defaultValue: () -> T,
        encode: (T) -> Set<String>,
        decode: (Set<String>) -> T
    ): DataStoreItem<T> =
        dataStoreItem(saver = ValueConvertible(stringSetPreferencesKey(name), defaultValue, encode, decode))



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

    protected inline fun <reified T: Enum<T>> itemEnum(
        name: String,
        defaultValue: T
    ): DataStoreItem<T> =
        itemStringConvertible(
            name = name,
            defaultValue = { defaultValue },
            encode = Enum<T>::name,
            decode = ::enumValueOf
        )

    protected inline fun <reified T: Enum<T>> itemEnumSet(
        name: String,
        noinline defaultValue: () -> Set<T> = ::emptySet
    ): DataStoreItem<Set<T>> =
        itemStringSetConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = { it.mapTo(mutableSetOf()) { it.name } },
            decode = { it.mapTo(EnumSet.noneOf(T::class.java)) { enumValueOf(it) } }
        )

    protected inline fun <reified T> Json.item(
        name: String,
        noinline defaultValue: () -> T
    ): DataStoreItem<T> =
        itemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = ::encodeToString,
            decode = ::decodeFromString
        )

    protected inline fun <reified T> Json.item(
        name: String,
        defaultValue: T
    ): DataStoreItem<T> = item(name = name, defaultValue = { defaultValue })

    protected inline fun <reified T: Any> Json.itemNullable(
        name: String
    ): DataStoreItem<T?> = item(name = name, defaultValue = { null })

    protected inline fun <reified T> Json.itemList(
        name: String,
        noinline defaultValue: () -> List<T> = ::emptyList
    ): DataStoreItem<List<T>> = item(name, defaultValue)

    protected inline fun <reified T> Json.itemSet(
        name: String,
        noinline defaultValue: () -> Set<T> = ::emptySet
    ): DataStoreItem<Set<T>> = item(name, defaultValue)

    protected inline fun <reified K, reified V> Json.itemMap(
        name: String,
        noinline defaultValue: () -> Map<K, V> = ::emptyMap
    ): DataStoreItem<Map<K, V>> = item(name, defaultValue)


    protected fun <T> DataStoreItem<T>.mapGetter(transform: (T) -> T): DataStoreItem<T> =
        dataStoreItem(saver = object : PreferencesSaver<T> by saver {
            override fun restore(prefs: Preferences): T = transform(saver.restore(prefs))
        })
}
