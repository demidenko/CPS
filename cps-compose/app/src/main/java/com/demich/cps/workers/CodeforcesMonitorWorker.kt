package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.demich.cps.NotificationChannels
import com.demich.cps.R
import com.demich.cps.notificationBuilder
import com.demich.datastore_itemized.ItemizedDataStore

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)
        val handle = monitor.handle()
        val contestId = monitor.contestId()

        //start monitor (launch)

        //subscribe to monitor data store

        return Result.success()
    }

    companion object {
        suspend fun startMonitor(context: Context, contestId: Int, handle: String) {
            val monitor = CodeforcesMonitorDataStore(context)

            val replace: Boolean
            if (contestId == monitor.contestId() && handle == monitor.handle()) {
                replace = false
            } else {
                replace = true
                monitor.handle(handle)
                monitor.contestId(contestId)
            }

            enqueueCodeforcesMonitorWorker(context, replace)
        }
    }
}

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by preferencesDataStore(name = "cf_monitor")
    }

    val contestId = itemIntNullable(name = "contest_id")
    val handle = itemString(name = "handle", defaultValue = "")
}