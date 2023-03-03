package com.demich.cps.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.demich.cps.contests.settings.settingsContests
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class UtilityWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "utility", context = context) {
            override suspend fun isEnabled() = true
            override val requestBuilder get() =
                PeriodicWorkRequestBuilder<UtilityWorker>(
                    repeatInterval = 24.hours.toJavaDuration()
                ).setConstraints(
                    Constraints(
                        requiresBatteryNotLow = true,
                        //TODO: requiresDeviceIdle = true
                    )
                )
        }
    }

    override suspend fun runWork(): Result {
        removeOldIgnoredContests()

        return Result.success()
    }

    private suspend fun removeOldIgnoredContests() {
        context.settingsContests.ignoredContests.update {
            it.filterValues { ignoredAtTime ->
                currentTime - ignoredAtTime < 30.days
            }
        }
    }
}