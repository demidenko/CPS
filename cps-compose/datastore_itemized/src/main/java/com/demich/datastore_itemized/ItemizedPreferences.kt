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

open class ItemizedPreferences internal constructor(
    protected open val preferences: Preferences
) {
    operator fun <T> get(item: DataStoreValue<T>): T =
        item.reader.restore(preferences)
}

class ItemizedMutablePreferences internal constructor(
    override val preferences: MutablePreferences
): ItemizedPreferences(preferences) {
    operator fun <T> set(item: DataStoreItem<T>, value: T) =
        item.saver.save(preferences, value)

    fun clear() {
        preferences.clear()
    }

    fun <T> remove(item: DataStoreItem<T>) {
        item.saver.removeFrom(preferences)
    }
}

// fun in prefs does not compile
context(prefs: ItemizedPreferences)
val <T> DataStoreValue<T>.value: T
    get() = prefs.get(item = this)

context(prefs: ItemizedMutablePreferences)
var <T> DataStoreItem<T>.value: T
    get() = prefs.get(item = this)
    set(value) = prefs.set(item = this, value = value)


fun <D: ItemizedDataStore, R> D.flowOf(
    transform: context(ItemizedPreferences) D.() -> R
): Flow<R> =
    dataStore.data
        .map { transform(ItemizedPreferences(it), this) }
        .distinctUntilChanged()

@OptIn(ExperimentalContracts::class)
suspend inline fun <D: ItemizedDataStore, R> D.fromSnapshot(
    block: context(ItemizedPreferences) D.() -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(snapshot(), this)
}

suspend fun <D: ItemizedDataStore> D.edit(
    block: D.(ItemizedMutablePreferences) -> Unit
) {
    dataStore.edit {
        block(ItemizedMutablePreferences(it))
    }
}