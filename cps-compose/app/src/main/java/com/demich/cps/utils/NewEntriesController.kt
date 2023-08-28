package com.demich.cps.utils

import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged


enum class NewEntryType {
    UNSEEN,
    SEEN,
    OPENED
}

typealias NewEntriesTypes = Map<Int, NewEntryType>

private fun NewEntriesTypes.getType(blogEntryId: Int): NewEntryType =
    this[blogEntryId] ?: NewEntryType.UNSEEN

class NewEntriesDataStoreItem (
    private val item: DataStoreItem<NewEntriesTypes>
) {
    val flow get() = item.flow

    suspend fun apply(newEntries: Collection<Int>) {
        if (newEntries.isEmpty()) return //TODO: is this OK/enough?
        item.update { old ->
            newEntries.associateWith { id -> old.getType(id) }
        }
    }

    suspend fun mark(id: Int, type: NewEntryType) {
        item.edit { this.markAtLeast(id, type) }
    }

    suspend fun markAtLeast(ids: List<Int>, type: NewEntryType) {
        if (ids.isEmpty()) return
        item.edit {
            for (id in ids) this.markAtLeast(id, type)
        }
    }

    private fun MutableMap<Int, NewEntryType>.markAtLeast(id: Int, type: NewEntryType) {
        val old = getType(id)
        if (type > old) this[id] = type
    }
}

data class NewEntryTypeCounters(
    val unseenCount: Int,
    val seenCount: Int
)

fun combineToCounters(flowOfIds: Flow<List<Int>>, flowOfTypes: Flow<NewEntriesTypes>) =
    combine(flowOfIds, flowOfTypes) { ids, types ->
        NewEntryTypeCounters(
            unseenCount = ids.count { types.getType(it) == NewEntryType.UNSEEN },
            seenCount = ids.count { types.getType(it) == NewEntryType.SEEN }
        )
    }.distinctUntilChanged()