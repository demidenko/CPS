package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.codeforces.CodeforcesNewEntriesDataStore
import com.demich.cps.contests.ContestsInfoDataStore
import kotlin.time.Duration.Companion.days

class UtilityWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "utility", context = context) {
            override suspend fun isEnabled() = true
            override val requestBuilder get() =
                CPSPeriodicWorkRequestBuilder<UtilityWorker>(
                    repeatInterval = 7.days,
                    requiresCharging = true,
                    requireNetwork = false
                )
        }
    }

    override suspend fun runWork(): Result {
        removeOldIgnoredContests()
        removeOldCodeforcesNewEntries()

        return Result.success()
    }

    private suspend fun removeOldIgnoredContests() {
        ContestsInfoDataStore(context).ignoredContests.update {
            it.filterValues { ignoredAtTime ->
                workerStartTime - ignoredAtTime < 30.days
            }
        }
    }

    private suspend fun removeOldCodeforcesNewEntries() {
        with(CodeforcesNewEntriesDataStore(context)) {
            mainNewEntries.removeOldMarkedItems()
            lostNewEntries.removeOldMarkedItems()
        }
    }
}