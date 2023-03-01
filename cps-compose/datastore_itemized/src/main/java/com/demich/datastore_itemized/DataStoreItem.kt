package com.demich.datastore_itemized

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface DataStoreItem<T> {
    val flow: Flow<T>

    //getter
    suspend operator fun invoke(): T

    //setter
    suspend operator fun invoke(newValue: T)

    suspend fun update(transform: (T) -> T)
}

internal abstract class DataStoreBaseItem<T, S: Any>(
    private val dataStore: DataStore<Preferences>,
    val key: Preferences.Key<S>
): DataStoreItem<T> {
    protected abstract fun fromPrefs(s: S?): T
    protected abstract fun toPrefs(t: T & Any): S

    override val flow: Flow<T>
        get() = dataStore.data.map { it[key] }.distinctUntilChanged().map(::fromPrefs)

    internal fun getValueFrom(prefs: Preferences): T = fromPrefs(prefs[key])

    override suspend operator fun invoke(): T = getValueFrom(dataStore.data.first())

    override suspend operator fun invoke(newValue: T) {
        dataStore.edit { prefs -> prefs.setValue(newValue) }
    }

    override suspend fun update(transform: (T) -> T) {
        dataStore.edit { prefs -> prefs.setValue(transform(getValueFrom(prefs))) }
    }

    private fun MutablePreferences.setValue(newValue: T) {
        if (newValue == null) remove(key)
        else set(key, toPrefs(newValue))
    }
}

internal class Item<T: Any> (
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<T>,
    private val defaultValue: T
): DataStoreBaseItem<T, T>(dataStore, key) {
    override fun fromPrefs(s: T?): T = s ?: defaultValue
    override fun toPrefs(t: T): T = t
}

internal class ItemNullable<T: Any> (
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<T>
): DataStoreBaseItem<T?, T>(dataStore, key) {
    override fun fromPrefs(s: T?): T? = s
    override fun toPrefs(t: T): T = t
}

internal class ItemConvertible<S: Any, T> (
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<S>,
    private val defaultValue: T,
    private val encode: (T) -> S,
    private val decode: (S) -> T
): DataStoreBaseItem<T, S>(dataStore, key) {
    override fun fromPrefs(s: S?): T = s?.runCatching(decode)?.getOrNull() ?: defaultValue
    override fun toPrefs(t: T & Any): S = encode(t)
}