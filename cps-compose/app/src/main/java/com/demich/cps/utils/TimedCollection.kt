package com.demich.cps.utils

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

    fun removeOld(timeThreshold: Instant): TimedCollection<T> =
        TimedCollection(m.filterValues { it < timeThreshold })
}

fun <T> emptyTimedCollection(): TimedCollection<T> = TimedCollection()

/*
basic
    - add item with date / instant
    - remove old items
    - Iterable on items

cf datastore upsolvingSuggestedProblems
    - get items
    - remove items by predicate
    - get items sorted by time

cf datastore monitorCanceledContests
    - get items as view

ContestsInfoDataStore ignoredContests
    - get items views
    - empty collection
    - get size
*/