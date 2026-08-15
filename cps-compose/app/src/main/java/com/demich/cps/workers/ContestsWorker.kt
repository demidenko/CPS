package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.contests.contestsFetchFlows
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.loading_engine.collectTo
import com.demich.cps.contests.settings.settingsContests
import kotlinx.coroutines.coroutineScope

class ContestsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override val workName get() = "contests"

        override fun getWork(context: Context) = object : CPSPeriodicWork(name = workName, context = context) {
            override suspend fun requestBuilder(): PeriodicWorkRequest.Builder? {
                val interval = context.settingsContests.autoUpdateInterval() ?: return null

                return CPSPeriodicWorkRequestBuilder<ContestsWorker>(
                    repeatInterval = interval,
                    batteryNotLow = true
                )
            }
        }
    }

    override suspend fun runWork() {
        // usual reload
        coroutineScope {
            context.settingsContests.contestsFetchFlows().collectTo(
                repository = context.contestsRepository
            )
        }
    }
}