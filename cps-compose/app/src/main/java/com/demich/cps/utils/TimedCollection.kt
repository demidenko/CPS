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

    fun withoutOld(timeThreshold: Instant): TimedCollection<T> =
        withoutOld { it < timeThreshold }

    fun withoutOld(isOld: (Instant) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterValues { !isOld(it) })

    fun without(predicate: (T) -> Boolean): TimedCollection<T> =
        TimedCollection(m.filterKeys { !predicate(it) })
}

fun <T> emptyTimedCollection(): TimedCollection<T> = TimedCollection()

suspend fun <T> DataStoreItem<TimedCollection<T>>.add(item: T, time: Instant) {
    update { it.add(item, time) }
}
