package com.demich.datastore_itemized

@JvmName("editList")
suspend inline fun<T> DataStoreItem<List<T>>.edit(crossinline block: MutableList<T>.() -> Unit) {
    update { it.toMutableList().apply(block) }
}

@JvmName("editSet")
suspend inline fun<T> DataStoreItem<Set<T>>.edit(crossinline block: MutableSet<T>.() -> Unit) {
    update { it.toMutableSet().apply(block) }
}

@JvmName("editMap")
suspend inline fun<K, V> DataStoreItem<Map<K, V>>.edit(crossinline block: MutableMap<K,V>.() -> Unit) {
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