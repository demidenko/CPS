package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.contests.ContestsInfoDataStore
import com.demich.cps.contests.ContestsReloader
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loading.asContestsReceiver
import com.demich.cps.contests.settings.settingsContests

class ContestsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
), ContestsReloader {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "contests", context = context) {
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
        //usual reload
        reloadEnabledPlatforms(
            settings = context.settingsContests,
            contestsInfo = ContestsInfoDataStore(context),
            contestsReceiver = context.contestsListDao.asContestsReceiver()
        )

        return Result.success()
    }
}