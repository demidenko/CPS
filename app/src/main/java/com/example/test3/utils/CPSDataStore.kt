package com.example.test3.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class CPSDataStore(protected val dataStore: DataStore<Preferences>) {

    abstract inner class CPSDataStoreItem<T, S> {
        abstract val key: Preferences.Key<S>
        protected abstract fun fromPrefs(s: S?): T
        //TODO [toPrefs(t: T!!): S] in 1.6??
        protected abstract fun toPrefs(t: T): S

        val flow: Flow<T> = dataStore.data.map { fromPrefs(it[key]) }.distinctUntilChanged()

        //getter
        suspend operator fun invoke(): T = fromPrefs(dataStore.data.first()[key])

        //setter
        suspend operator fun invoke(newValue: T) {
            dataStore.edit { prefs ->
                newValue?.let { prefs[key] = toPrefs(it) } ?: prefs.remove(key)
            }
        }
    }

    inner class Item<T> (
        override val key: Preferences.Key<T>,
        private val defaultValue: T
    ): CPSDataStoreItem<T, T>() {
        override fun fromPrefs(s: T?): T = s ?: defaultValue
        override fun toPrefs(t: T): T = t
    }

    inner class ItemNullable<T> (
        override val key: Preferences.Key<T>
    ): CPSDataStoreItem<T?, T>() {
        override fun fromPrefs(s: T?): T? = s
        override fun toPrefs(t: T?): T = t!!
    }

    inner class ItemEnum<T: Enum<T>> (
        name: String,
        private val clazz: Class<T>,
        private val defaultValueCallback: () -> T
    ): CPSDataStoreItem<T, String>() {
        constructor(name: String, defaultValue: T): this(
            name, defaultValue.javaClass, defaultValueCallback = { defaultValue }
        )
        override val key = stringPreferencesKey(name)

        override fun fromPrefs(s: String?): T {
            return s?.let { str ->
                clazz.enumConstants.first { it.name == str }
            } ?: defaultValueCallback()
        }

        override fun toPrefs(t: T): String = t.name
    }

    inner class ItemStringConvertible<T> (
        name: String,
        private val defaultValue: T,
        private val encode: (T) -> String,
        private val decode: (String) -> T,
    ): CPSDataStoreItem<T, String>() {
        override val key = stringPreferencesKey(name)
        override fun fromPrefs(s: String?): T = s?.let(decode) ?: defaultValue
        override fun toPrefs(t: T): String = encode(t)
    }

    inline fun<reified T> Json.itemStringConvertible(name: String, defaultValue: T) =
        ItemStringConvertible(
            name = name,
            defaultValue = defaultValue,
            encode = ::encodeToString,
            decode = ::decodeFromString
        )
}