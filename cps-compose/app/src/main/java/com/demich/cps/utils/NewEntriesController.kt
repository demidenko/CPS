package com.demich.cps.utils


enum class NewEntryType {
    UNSEEN,
    SEEN,
    OPENED
}

typealias NewEntriesTypes = Map<String, NewEntryType>

class NewEntriesController(
    private val item: CPSDataStoreItem<NewEntriesTypes>
) {
    suspend fun apply(newEntries: Collection<String>) {
        if (newEntries.isEmpty()) return //TODO: is this OK/enough?
        item.updateValue { old ->
            newEntries.associateWith { id -> old[id] ?: NewEntryType.UNSEEN }
        }
    }

    suspend fun mark(id: String, type: NewEntryType) {
        item.edit { this.markAtLeast(id, type) }
    }

    suspend fun markAtLeast(ids: List<String>, type: NewEntryType) {
        if (ids.isEmpty()) return
        item.edit {
            for (id in ids) this.markAtLeast(id, type)
        }
    }

    private fun MutableMap<String, NewEntryType>.markAtLeast(id: String, type: NewEntryType) {
        val old = this[id] ?: NewEntryType.UNSEEN
        if (type > old) this[id] = type
    }
}
