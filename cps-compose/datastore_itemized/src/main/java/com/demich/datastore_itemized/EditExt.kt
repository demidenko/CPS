package com.demich.datastore_itemized

@JvmName("editList")
suspend fun<T> DataStoreItem<List<T>>.edit(block: MutableList<T>.() -> Unit) {
    update { it.toMutableList().apply(block) }
}

@JvmName("editSet")
suspend fun<T> DataStoreItem<Set<T>>.edit(block: MutableSet<T>.() -> Unit) {
    update { it.toMutableSet().apply(block) }
}

@JvmName("editMap")
suspend fun<K, V> DataStoreItem<Map<K, V>>.edit(block: MutableMap<K,V>.() -> Unit) {
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