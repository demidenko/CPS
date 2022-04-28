package com.demich.cps.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

interface CPSDataStoreItem<T> {
    val key: Preferences.Key<*>
    val flow: Flow<T>

    //getter
    suspend operator fun invoke(): T

    //setter
    suspend operator fun invoke(newValue: T)
}


abstract class CPSDataStore(protected val dataStore: DataStore<Preferences>) {

    protected abstract class DataStoreItem<T, S>(
        private val dataStore: DataStore<Preferences>
    ): CPSDataStoreItem<T> {
        abstract override val key: Preferences.Key<S>
        protected abstract fun fromPrefs(s: S?): T
        //TODO [toPrefs(t: T & Any): S] in kotlin 1.7??
        protected abstract fun toPrefs(t: T): S

        override val flow: Flow<T>
            get() = dataStore.data.map { fromPrefs(it[key]) }.distinctUntilChanged()
            //get() = dataStore.data.map { it[key] }.distinctUntilChanged().map { fromPrefs(it) }
            //get() = dataStore.data.distinctUntilChangedBy { it[key] }.map { fromPrefs(it[key]) }

        override suspend operator fun invoke(): T = fromPrefs(dataStore.data.first()[key])

        override suspend operator fun invoke(newValue: T) {
            dataStore.edit { prefs ->
                newValue?.let { prefs[key] = toPrefs(it) } ?: prefs.remove(key)
            }
        }
    }

    private class Item<T> (
        dataStore: DataStore<Preferences>,
        override val key: Preferences.Key<T>,
        private val defaultValue: T
    ): DataStoreItem<T, T>(dataStore) {
        override fun fromPrefs(s: T?): T = s ?: defaultValue
        override fun toPrefs(t: T): T = t
    }

    private class ItemNullable<T> (
        dataStore: DataStore<Preferences>,
        override val key: Preferences.Key<T>
    ): DataStoreItem<T?, T>(dataStore) {
        override fun fromPrefs(s: T?): T? = s
        override fun toPrefs(t: T?): T = t!!
    }

    private class ItemStringConvertible<T> (
        dataStore: DataStore<Preferences>,
        name: String,
        private val defaultValue: T,
        private val encode: (T) -> String,
        private val decode: (String) -> T,
    ): DataStoreItem<T, String>(dataStore) {
        override val key = stringPreferencesKey(name)
        override fun fromPrefs(s: String?): T = s?.let(decode) ?: defaultValue
        override fun toPrefs(t: T): String = encode(t)
    }

    private fun<T> item(key: Preferences.Key<T>, defaultValue: T): CPSDataStoreItem<T> =
        Item(key = key, defaultValue = defaultValue, dataStore = dataStore)

    private fun<T> itemNullable(key: Preferences.Key<T>): CPSDataStoreItem<T?> =
        ItemNullable(key = key, dataStore = dataStore)

    protected fun<T> itemStringConvertible(name: String, defaultValue: T, encode: (T) -> String, decode: (String) -> T): CPSDataStoreItem<T> =
        ItemStringConvertible(name = name, defaultValue = defaultValue, encode = encode, decode = decode, dataStore = dataStore)



    protected fun itemBoolean(name: String, defaultValue: Boolean): CPSDataStoreItem<Boolean> =
        item(booleanPreferencesKey(name), defaultValue)

    protected fun itemInt(name: String, defaultValue: Int): CPSDataStoreItem<Int> =
        item(intPreferencesKey(name), defaultValue)

    protected fun itemIntNullable(name: String): CPSDataStoreItem<Int?> =
        itemNullable(intPreferencesKey(name))

    protected fun itemLong(name: String, defaultValue: Long): CPSDataStoreItem<Long> =
        item(longPreferencesKey(name), defaultValue)

    protected fun itemLongNullable(name: String): CPSDataStoreItem<Long?> =
        itemNullable(longPreferencesKey(name))

    protected fun itemString(name: String, defaultValue: String): CPSDataStoreItem<String> =
        item(stringPreferencesKey(name), defaultValue)

    protected fun itemStringNullable(name: String): CPSDataStoreItem<String?> =
        itemNullable(stringPreferencesKey(name))

    protected inline fun<reified T: Enum<T>> itemEnum(name: String, defaultValue: T): CPSDataStoreItem<T> =
        itemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = { it.name },
            decode = ::enumValueOf
        )

    protected inline fun<reified T: Enum<T>> itemEnumSet(name: String, defaultValue: Set<T> = emptySet()): CPSDataStoreItem<Set<T>> =
        object : DataStoreItem<Set<T>, Set<String>>(dataStore) {
            override val key = stringSetPreferencesKey(name)
            override fun fromPrefs(s: Set<String>?) = s?.mapTo(mutableSetOf()) { enumValueOf<T>(it) } ?: defaultValue
            override fun toPrefs(t: Set<T>) = t.mapTo(mutableSetOf()) { it.name }
        }

    protected inline fun<reified T> itemJsonable(name: String, defaultValue: T): CPSDataStoreItem<T> =
        itemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = jsonCPS::encodeToString,
            decode = jsonCPS::decodeFromString
        )

}

suspend fun<T> CPSDataStoreItem<List<T>>.add(value: T) {
    invoke(newValue = invoke() + value)
}