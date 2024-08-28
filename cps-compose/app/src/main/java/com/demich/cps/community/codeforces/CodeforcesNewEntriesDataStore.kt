package com.demich.cps.community.codeforces

import android.content.Context
import com.demich.cps.utils.*
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper

class CodeforcesNewEntriesDataStore(context: Context): ItemizedDataStore(context.cf_new_entries_dataStore) {
    companion object {
        private val Context.cf_new_entries_dataStore by dataStoreWrapper("cf_new_entries")
    }

    val commonNewEntries = itemNewEntriesTypes(name = "common")
    //val mainNewEntries = itemNewEntriesTypes(name = "main")
    //val lostNewEntries = itemNewEntriesTypes(name = "lost")

    private fun itemNewEntriesTypes(name: String) = NewEntriesDataStoreItem(jsonCPS.itemMap(name = name))
}
