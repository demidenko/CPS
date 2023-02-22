package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.contests.ContestsReloader
import com.demich.cps.contests.loaders.ContestsReceiver
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.room.contestsListDao
import kotlin.time.Duration.Companion.days
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
            override suspend fun isEnabled() = true //TODO: manage in settings
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<ContestsWorker>(
                    repeatInterval = 1.hours,
                    batteryNotLow = true
                )
        }
    }
    override suspend fun runWork(): Result {
        val settings = context.settingsContests

        //remove old ignored items
        settings.ignoredContests.update {
            it.filterValues { ignoredAtTime ->
                currentTime - ignoredAtTime < 30.days
            }
        }

        //usual reload
        reloadEnabledPlatforms(
            settings = settings,
            contestsReceiver = ContestsReceiver(
                dao = context.contestsListDao,
                setLoadingStatus = { _, _ -> },
                consumeError = { _, _, _ -> },
                clearErrors = { }
            )
        )

        return Result.success()
    }

}