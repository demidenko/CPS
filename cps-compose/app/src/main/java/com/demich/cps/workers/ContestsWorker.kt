package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.contests.ContestsReloader
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.loading.asContestsReceiver
import com.demich.cps.contests.settings.settingsContests
import kotlin.time.Duration.Companion.hours

class ContestsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
), ContestsReloader {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "contests", context = context) {
            override suspend fun isEnabled() = context.settingsContests.enabledAutoUpdate()
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<ContestsWorker>(
                    repeatInterval = 1.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        //usual reload
        reloadEnabledPlatforms(
            settings = context.settingsContests,
            contestsReceiver = context.contestsListDao.asContestsReceiver()
        )

        return Result.success()
    }
}