package com.demich.cps.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


enum class NewEntryType {
    UNSEEN,
    //TODO: SEEN,
    OPENED
}

typealias NewEntriesTypes = Map<String, NewEntryType>

class NewEntriesController(
    private val item: CPSDataStoreItem<NewEntriesTypes>
) {
    suspend fun apply(newEntries: Collection<String>) {
        if (newEntries.isEmpty()) return //TODO: is this OK/enough?
        item.updateValue { old ->
            newEntries.associateWith { id ->
                when (old[id]) {
                    NewEntryType.UNSEEN, null -> NewEntryType.UNSEEN
                    NewEntryType.OPENED -> NewEntryType.OPENED
                }
            }
        }
    }

    suspend fun mark(id: String, type: NewEntryType) {
        item.edit {
            this[id] = type
        }
    }

    fun flowOfUnseenCount(): Flow<Int> = item.flow.map { m ->
        m.count { it.value == NewEntryType.UNSEEN }
    }

    fun flowOfUnopenedCount(): Flow<Int> = item.flow.map { m ->
        m.count { it.value != NewEntryType.OPENED }
    }
}
