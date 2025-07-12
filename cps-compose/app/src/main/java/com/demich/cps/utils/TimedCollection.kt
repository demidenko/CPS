package com.demich.cps.utils

import com.demich.datastore_itemized.DataStoreItem
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TimedCollection<T>(
    private val m: Map<T, Instant> = emptyMap()
): Collection<T> by m.keys {
    fun valuesSortedByTime(): List<T> =
        m.entries.sortedBy { it.value }.map { it.key }

    fun add(value: T, time: Instant): TimedCollection<T> =
        TimedCollection(m.plus(value to time))

    fun internalFilterByTime(predicate: (Instant) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterValues(predicate))

    fun filterByValue(predicate: (T) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterKeys(predicate))
}

fun <T> emptyTimedCollection(): TimedCollection<T> = TimedCollection()

suspend fun <T> DataStoreItem<TimedCollection<T>>.add(value: T, time: Instant) {
    update { it.add(value, time) }
}

suspend fun <T> DataStoreItem<TimedCollection<T>>.removeOlderThan(time: Instant) {
    removeOld { it < time }
}

suspend inline fun <T> DataStoreItem<TimedCollection<T>>.removeOld(crossinline isOld: (Instant) -> Boolean) {
    update { it.internalFilterByTime { time -> !isOld(time) } }
}