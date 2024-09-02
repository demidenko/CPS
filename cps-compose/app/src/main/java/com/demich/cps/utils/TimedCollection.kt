package com.demich.cps.utils

import com.demich.datastore_itemized.DataStoreItem
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
class TimedCollection<T>(
    private val m: Map<T, Instant> = emptyMap()
): Collection<T> by m.keys {
    fun itemsSortedByTime(): List<T> =
        m.entries.sortedBy { it.value }.map { it.key }

    fun add(item: T, time: Instant): TimedCollection<T> =
        TimedCollection(m.plus(item to time))

    fun internalFilterByTime(predicate: (Instant) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterValues(predicate))

    fun filterByValue(predicate: (T) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterKeys(predicate))
}

fun <T> emptyTimedCollection(): TimedCollection<T> = TimedCollection()

suspend fun <T> DataStoreItem<TimedCollection<T>>.add(item: T, time: Instant) {
    update { it.add(item, time) }
}

suspend fun <T> DataStoreItem<TimedCollection<T>>.removeOlderThan(time: Instant) {
    removeOld { it < time }
}

suspend fun <T> DataStoreItem<TimedCollection<T>>.removeOld(isOld: (Instant) -> Boolean) {
    update { it.internalFilterByTime { time -> !isOld(time) } }
}