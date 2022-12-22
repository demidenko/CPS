package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.runMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)
        val handle = monitor.handle()
        val contestId = monitor.contestId()

        withContext(Dispatchers.IO) {
            launch {
                monitor.runMonitor()
            }
        }

        //subscribe to monitor data store

        return Result.success()
    }
}