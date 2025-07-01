package com.demich.cps.utils

import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable


enum class NewEntryType {
    UNSEEN,
    SEEN,
    OPENED
}

@Serializable
data class NewEntryInfo(
    val type: NewEntryType,
    val date: LocalDate
)

fun Map<Int, NewEntryInfo>.getType(id: Int): NewEntryType =
    this[id]?.type ?: NewEntryType.UNSEEN

private fun MutableMap<Int, NewEntryInfo>.markAtLeast(
    id: Int,
    type: NewEntryType,
    date: LocalDate
) {
    val oldType = getType(id)
    if (type > oldType) set(key = id, value = NewEntryInfo(type, date))
}

class NewEntriesDataStoreItem (
    private val item: DataStoreItem<Map<Int, NewEntryInfo>>
) {
    val flow get() = item.asFlow()

    private fun getCurrentDate(): LocalDate = getCurrentTime().toSystemDateTime().date

    suspend fun markAtLeast(id: Int, type: NewEntryType) = markAtLeast(listOf(id), type)

    suspend fun markAtLeast(ids: List<Int>, type: NewEntryType) {
        if (ids.isEmpty()) return
        val date = getCurrentDate()
        item.edit {
            for (id in ids) markAtLeast(id, type, date)
        }
    }

    suspend fun removeOlderThan(days: Int) {
        val currentDate = getCurrentDate()
        item.update { types ->
            types.filterValues { it.date.daysUntil(currentDate) <= days }
        }
    }
}

data class NewEntryTypeCounters(
    val unseenCount: Int,
    val seenCount: Int
)

fun combineToCounters(flowOfIds: Flow<List<Int>>, flowOfTypes: Flow<Map<Int, NewEntryInfo>>) =
    combine(flowOfIds, flowOfTypes) { ids, types ->
        NewEntryTypeCounters(
            unseenCount = ids.count { types.getType(it) == NewEntryType.UNSEEN },
            seenCount = ids.count { types.getType(it) == NewEntryType.SEEN }
        )
    }.distinctUntilChanged()