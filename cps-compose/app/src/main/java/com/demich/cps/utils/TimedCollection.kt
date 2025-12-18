package com.demich.cps.utils

import com.demich.datastore_itemized.DataStoreEditScope
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.value
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TimedCollection<T>(
    private val m: Map<T, Instant> = emptyMap()
): Collection<T> by m.keys {
    fun valuesSortedByTime(): List<T> =
        m.entries.sortedBy { it.value }.map { it.key }

    fun plus(value: T, time: Instant): TimedCollection<T> =
        TimedCollection(m.plus(value to time))

    fun internalFilterByTime(predicate: (Instant) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterValues(predicate))

    fun filterValues(predicate: (T) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterKeys(predicate))
}

fun <T> emptyTimedCollection(): TimedCollection<T> = TimedCollection()

suspend fun <T> DataStoreItem<TimedCollection<T>>.add(value: T, time: Instant) {
    update { it.plus(value, time) }
}

context(scope: DataStoreEditScope)
fun <T> DataStoreItem<TimedCollection<T>>.removeOlderThan(time: Instant) {
    removeOld { it < time }
}

context(scope: DataStoreEditScope)
inline fun <T> DataStoreItem<TimedCollection<T>>.removeOld(crossinline isOld: (Instant) -> Boolean) {
    value = value.internalFilterByTime { time -> !isOld(time) }
}