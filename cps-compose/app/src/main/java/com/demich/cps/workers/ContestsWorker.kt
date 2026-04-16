package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.contests.contestsFetchFlows
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.loading_engine.collectResults
import com.demich.cps.contests.settings.settingsContests

class ContestsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "contests", context = context) {
            private val settings get() = context.settingsContests

            override suspend fun isEnabled() = settings.autoUpdateInterval() != null

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<ContestsWorker>(
                    repeatInterval = requireNotNull(settings.autoUpdateInterval()),
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        // usual reload
        context.contestsRepository.collectResults(
            flows = context.settingsContests.contestsFetchFlows()
        )

        return Result.success()
    }
}