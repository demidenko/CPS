package com.demich.cps.utils

import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreValue
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
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

typealias NewEntriesMap = Map<Int, NewEntryInfo>

fun NewEntriesMap.getType(id: Int): NewEntryType =
    this[id]?.type ?: UNSEEN

private fun MutableMap<Int, NewEntryInfo>.markAtLeast(
    id: Int,
    type: NewEntryType,
    date: LocalDate
) {
    val oldType = getType(id)
    if (type > oldType) set(key = id, value = NewEntryInfo(type, date))
}

private fun systemDate(): LocalDate = getSystemTime().toLocalDateTime(TimeZone.UTC).date

class NewEntriesDataStoreItem (
    private val item: DataStoreItem<NewEntriesMap>
): DataStoreValue<NewEntriesMap>(item) {

    suspend fun markAtLeast(id: Int, type: NewEntryType) = markAtLeast(listOf(id), type)

    suspend fun markAtLeast(ids: Collection<Int>, type: NewEntryType) {
        if (ids.isEmpty()) return
        item.edit {
            val date = systemDate()
            for (id in ids) markAtLeast(id, type, date)
        }
    }

    suspend fun removeOlderThan(days: Int) {
        item.update { types ->
            val date = systemDate()
            types.filterValues { it.date.daysUntil(date) <= days }
        }
    }
}

data class NewEntryTypeCounters(
    val unseenCount: Int,
    val seenCount: Int
)

fun combineToCounters(flowOfIds: Flow<List<Int>>, flowOfTypes: Flow<NewEntriesMap>) =
    combine(flowOfIds, flowOfTypes) { ids, types ->
        NewEntryTypeCounters(
            unseenCount = ids.count { types.getType(it) == UNSEEN },
            seenCount = ids.count { types.getType(it) == SEEN }
        )
    }.distinctUntilChanged()