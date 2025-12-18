package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class DataStoreSnapshot internal constructor(
    protected open val preferences: Preferences
) {
    operator fun <T> get(item: DataStoreValue<T>): T =
        item.reader.restore(preferences)
}

class DataStoreEditScope internal constructor(
    override val preferences: MutablePreferences
): DataStoreSnapshot(preferences) {
    operator fun <T> set(item: DataStoreItem<T>, value: T) =
        item.saver.save(preferences, value)

    fun clear() {
        preferences.clear()
    }

    fun <T> remove(item: DataStoreItem<T>) {
        item.saver.removeFrom(preferences)
    }
}

context(scope: DataStoreSnapshot)
val <T> DataStoreValue<T>.value: T
    get() = scope.get(item = this)

context(scope: DataStoreSnapshot)
operator fun <K, V> DataStoreValue<Map<K, V>>.get(key: K): V? =
    scope.get(item = this).get(key = key)

context(scope: DataStoreEditScope)
var <T> DataStoreItem<T>.value: T
    get() = scope.get(item = this)
    set(value) = scope.set(item = this, value = value)

fun <D: ItemizedDataStore, R> D.flowOf(
    transform: context(DataStoreSnapshot) D.() -> R
): Flow<R> =
    dataStore.data
        .map {
            with(DataStoreSnapshot(it)) {
                transform()
            }
        }
        .distinctUntilChanged()

@OptIn(ExperimentalContracts::class)
suspend inline fun <D: ItemizedDataStore, R> D.fromSnapshot(
    block: context(DataStoreSnapshot) D.() -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    with(snapshot()) {
        return block()
    }
}

suspend fun <D: ItemizedDataStore> D.edit(
    block: context(DataStoreEditScope) D.() -> Unit
) {
    dataStore.edit {
        with(DataStoreEditScope(it)) {
            block()
        }
    }
}