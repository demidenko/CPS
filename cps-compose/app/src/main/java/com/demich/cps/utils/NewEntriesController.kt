package com.demich.cps.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


enum class NewEntryType {
    UNSEEN,
    SEEN,
    OPENED
}

class NewEntriesController(
    private val item: CPSDataStoreItem<Map<String,NewEntryType>>
) {
    suspend fun apply(newEntries: Collection<String>, frontDoor: Boolean) {
        item.updateValue { old ->
            newEntries.associateWith { id ->
                when (old[id]) {
                    NewEntryType.UNSEEN, null -> NewEntryType.UNSEEN
                    NewEntryType.SEEN -> {
                        if (frontDoor) NewEntryType.OPENED
                        else NewEntryType.SEEN
                    }
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
