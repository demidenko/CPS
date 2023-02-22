package com.demich.datastore_itemized

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DataStoreWrapper(
    internal val dataStore: DataStore<Preferences>
)

fun dataStoreWrapper(
    name: String
) = object : ReadOnlyProperty<Context, DataStoreWrapper> {
    val delegate = preferencesDataStore(name)
    override fun getValue(thisRef: Context, property: KProperty<*>): DataStoreWrapper {
        return DataStoreWrapper(delegate.getValue(thisRef, property))
    }
}

interface DataStoreItem<T> {
    val key: Preferences.Key<*>
    val flow: Flow<T>

    //getter
    suspend operator fun invoke(): T

    //setter
    suspend operator fun invoke(newValue: T)

    suspend fun update(transform: (T) -> T)
}


private abstract class DataStoreBaseItem<T, S: Any>(
    private val dataStore: DataStore<Preferences>,
    override val key: Preferences.Key<S>
): DataStoreItem<T> {
    protected abstract fun fromPrefs(s: S?): T
    protected abstract fun toPrefs(t: T & Any): S

    override val flow: Flow<T>
        get() = dataStore.data.map { it[key] }.distinctUntilChanged().map(::fromPrefs)

    override suspend operator fun invoke(): T = fromPrefs(dataStore.data.first()[key])

    override suspend operator fun invoke(newValue: T) {
        dataStore.edit { prefs -> prefs.setValue(newValue) }
    }

    override suspend fun update(transform: (T) -> T) {
        dataStore.edit { prefs -> prefs.setValue(transform(fromPrefs(prefs[key]))) }
    }

    private fun MutablePreferences.setValue(newValue: T) {
        if (newValue == null) remove(key)
        else set(key, toPrefs(newValue))
    }
}

private class Item<T: Any> (
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<T>,
    private val defaultValue: T
): DataStoreBaseItem<T, T>(dataStore, key) {
    override fun fromPrefs(s: T?): T = s ?: defaultValue
    override fun toPrefs(t: T): T = t
}

private class ItemNullable<T: Any> (
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<T>
): DataStoreBaseItem<T?, T>(dataStore, key) {
    override fun fromPrefs(s: T?): T? = s
    override fun toPrefs(t: T): T = t
}

private class ItemConvertible<S: Any, T> (
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<S>,
    private val defaultValue: T,
    private val encode: (T) -> S,
    private val decode: (S) -> T
): DataStoreBaseItem<T, S>(dataStore, key) {
    override fun fromPrefs(s: S?): T = s?.runCatching(decode)?.getOrNull() ?: defaultValue
    override fun toPrefs(t: T & Any): S = encode(t)
}


abstract class ItemizedDataStore(private val dataStore: DataStore<Preferences>) {

    constructor(wrapper: DataStoreWrapper): this(wrapper.dataStore)

    protected suspend fun resetItems(items: Collection<DataStoreItem<*>>) {
        if (items.isEmpty()) return
        dataStore.edit { prefs ->
            items.forEach { prefs.remove(it.key) }
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


@JvmName("editList")
suspend fun<T> DataStoreItem<List<T>>.edit(block: MutableList<T>.() -> Unit) {
    update { it.toMutableList().apply(block) }
}

@JvmName("editSet")
suspend fun<T> DataStoreItem<Set<T>>.edit(block: MutableSet<T>.() -> Unit) {
    update { it.toMutableSet().apply(block) }
}

@JvmName("editMap")
suspend fun<K, V> DataStoreItem<Map<K,V>>.edit(block: MutableMap<K,V>.() -> Unit) {
    update { it.toMutableMap().apply(block) }
}


@JvmName("addList")
suspend fun<T> DataStoreItem<List<T>>.add(value: T) {
    update { it + value }
}

@JvmName("addSet")
suspend fun<T> DataStoreItem<Set<T>>.add(value: T) {
    update { it + value }
}