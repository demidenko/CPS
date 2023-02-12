package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)

        withContext(Dispatchers.IO) {
            monitor.launchIn(scope = this) {
                //TODO notify
            }
        }

        //TODO: subscribe to monitor data store

        return Result.success()
    }
}