package com.demich.datastore_itemized

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T> DataStoreItem<T>.setValueIn(
    scope: CoroutineScope,
    value: T
) {
    scope.launch {
        setValue(value = value)
    }
}

fun <T> DataStoreItem<T>.updateIn(
    scope: CoroutineScope,
    transform: (T) -> T
) {
    scope.launch {
        update(transform)
    }
}