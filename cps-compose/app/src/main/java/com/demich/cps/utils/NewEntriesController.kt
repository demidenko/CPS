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

class NewEntriesDataStoreItem (
    private val item: DataStoreItem<NewEntriesTypes>
) {
    val flow get() = item.flow

    suspend fun apply(newEntries: Collection<Int>) {
        if (newEntries.isEmpty()) return //TODO: is this OK/enough?
        item.update { old ->
            newEntries.associateWith { id -> old[id] ?: NewEntryType.UNSEEN }
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
        val old = this[id] ?: NewEntryType.UNSEEN
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
            unseenCount = ids.count { (types[it] ?: NewEntryType.UNSEEN) == NewEntryType.UNSEEN },
            seenCount = ids.count { types[it] == NewEntryType.SEEN }
        )
    }.distinctUntilChanged()