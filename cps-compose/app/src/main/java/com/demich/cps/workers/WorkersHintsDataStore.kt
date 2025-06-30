package com.demich.cps.workers

import android.content.Context
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant


class WorkersHintsDataStore(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by dataStoreWrapper(name = "workers_hints")
    }

    val followLastSuccessTime = jsonCPS.itemNullable<Instant>(name = "follow_last_success")
}