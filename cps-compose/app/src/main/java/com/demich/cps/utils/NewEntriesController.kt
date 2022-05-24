package com.demich.cps.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged


enum class NewEntryType {
    UNSEEN,
    SEEN,
    OPENED
}

typealias NewEntriesTypes = Map<String, NewEntryType>

class NewEntriesDataStoreItem (
    item: CPSDataStoreItem<NewEntriesTypes>
): CPSDataStoreItem<NewEntriesTypes> by item {
    suspend fun apply(newEntries: Collection<String>) {
        if (newEntries.isEmpty()) return //TODO: is this OK/enough?
        updateValue { old ->
            newEntries.associateWith { id -> old[id] ?: NewEntryType.UNSEEN }
        }
    }

    suspend fun mark(id: String, type: NewEntryType) {
        edit { this.markAtLeast(id, type) }
    }

    suspend fun markAtLeast(ids: List<String>, type: NewEntryType) {
        if (ids.isEmpty()) return
        edit {
            for (id in ids) this.markAtLeast(id, type)
        }
    }

    private fun MutableMap<String, NewEntryType>.markAtLeast(id: String, type: NewEntryType) {
        val old = this[id] ?: NewEntryType.UNSEEN
        if (type > old) this[id] = type
    }
}

data class NewEntryTypeCounters(
    val unseenCount: Int,
    val seenCount: Int
)

fun combineToCounters(flowOfIds: Flow<List<String>>, flowOfTypes: Flow<NewEntriesTypes>) =
    combine(flowOfIds, flowOfTypes) { ids, types ->
        NewEntryTypeCounters(
            unseenCount = ids.count { types[it] == NewEntryType.UNSEEN },
            seenCount = ids.count { types[it] == NewEntryType.SEEN }
        )
    }.distinctUntilChanged()