package com.demich.cps.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

interface CPSDataStoreItem<T> {
    val flow: Flow<T>
    suspend operator fun invoke(): T
    suspend operator fun invoke(newValue: T)
}

abstract class CPSDataStore(protected val dataStore: DataStore<Preferences>) {

    abstract inner class DataStoreItem<T, S>: CPSDataStoreItem<T> {
        abstract val key: Preferences.Key<S>
        protected abstract fun fromPrefs(s: S?): T
        //TODO [toPrefs(t: T & Any): S] in kotlin 1.7??
        protected abstract fun toPrefs(t: T): S

        override val flow: Flow<T>
            get() = dataStore.data.map { fromPrefs(it[key]) }.distinctUntilChanged()
            //get() = dataStore.data.map { it[key] }.distinctUntilChanged().map { fromPrefs(it) }
            //get() = dataStore.data.distinctUntilChangedBy { it[key] }.map { fromPrefs(it[key]) }

        //getter
        override suspend operator fun invoke(): T = fromPrefs(dataStore.data.first()[key])

        //setter
        override suspend operator fun invoke(newValue: T) {
            dataStore.edit { prefs ->
                newValue?.let { prefs[key] = toPrefs(it) } ?: prefs.remove(key)
            }
        }
    }

    inner class Item<T> (
        override val key: Preferences.Key<T>,
        private val defaultValue: T
    ): DataStoreItem<T, T>() {
        override fun fromPrefs(s: T?): T = s ?: defaultValue
        override fun toPrefs(t: T): T = t
    }

    inner class ItemNullable<T> (
        override val key: Preferences.Key<T>
    ): DataStoreItem<T?, T>() {
        override fun fromPrefs(s: T?): T? = s
        override fun toPrefs(t: T?): T = t!!
    }

    inner class ItemStringConvertible<T> (
        name: String,
        private val defaultValue: T,
        private val encode: (T) -> String,
        private val decode: (String) -> T,
    ): DataStoreItem<T, String>() {
        override val key = stringPreferencesKey(name)
        override fun fromPrefs(s: String?): T = s?.let(decode) ?: defaultValue
        override fun toPrefs(t: T): String = encode(t)
    }

    inline fun<reified T: Enum<T>> itemEnum(name: String, defaultValue: T) =
        ItemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = { it.name },
            decode = ::enumValueOf
        )

    inline fun<reified T: Enum<T>> itemEnumSet(name: String) =
        object : DataStoreItem<Set<T>, Set<String>>() {
            override val key = stringSetPreferencesKey(name)
            override fun fromPrefs(s: Set<String>?) = s?.mapTo(mutableSetOf()) { enumValueOf<T>(it) } ?: emptySet()
            override fun toPrefs(t: Set<T>) = t.mapTo(mutableSetOf()) { it.name }
        }

    inline fun<reified T> itemJsonConvertible(name: String, defaultValue: T) =
        ItemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = jsonCPS::encodeToString,
            decode = jsonCPS::decodeFromString
        )

}