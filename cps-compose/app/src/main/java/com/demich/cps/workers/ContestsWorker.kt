package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.contests.settings.settingsContests
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ContestsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "contests", context = context) {
            override suspend fun isEnabled() = true //TODO: manage in settings
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<ContestsWorker>(
                    repeatInterval = 24.hours,
                    batteryNotLow = true
                )
        }
    }
    override suspend fun runWork(): Result {

        //remove old ignored items
        context.settingsContests.ignoredContests.updateValue {
            it.filterValues { ignoredAtTime ->
                currentTime - ignoredAtTime < 30.days
            }
        }

        //TODO: usual reload

        return Result.success()
    }

}