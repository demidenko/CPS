package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.platforms.api.CodeforcesApi
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CodeforcesMonitorLauncherWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_monitor_launcher", context = context) {
            override suspend fun isEnabled() =
                CodeforcesAccountManager().getSettings(context).monitorEnabled()

            override val requestBuilder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesMonitorLauncherWorker>(
                    repeatInterval = 45.minutes
                )
        }

        suspend fun startMonitor(contestId: Int, handle: String, context: Context) {
            val monitor = CodeforcesMonitorDataStore(context)

            val replace: Boolean
            if (contestId == monitor.contestId() && handle == monitor.handle()) {
                replace = false
            } else {
                replace = true
                monitor.reset()
                monitor.handle(handle)
                monitor.contestId(contestId)
            }

            getCodeforcesMonitorWork(context).enqueue(replace)
        }
    }

    private fun isActual(time: Instant) = workerStartTime - time < 24.hours

    override suspend fun runWork(): Result {
        val dataStore = CodeforcesAccountManager().dataStore(context)

        val info = dataStore.getSavedInfo() ?: return Result.success()
        if (info.status != STATUS.OK) return Result.success()

        val newSubmissions = getNewSubmissions(
            handle = info.handle,
            lastSubmissionId = dataStore.monitorLastSubmissionId()
        )

        dataStore.apply {
            newSubmissions.firstOrNull()?.let {
                monitorLastSubmissionId(it.id)
            }

            monitorCanceledContests.update { list ->
                list.filter { isActual(it.second) }
            }

            newSubmissions.firstOrNull { submission ->
                submission.author.participantType.contestParticipant()
            }?.let { submission ->
                if (monitorCanceledContests().none { it.first == submission.contestId }) {
                    startMonitor(
                        contestId = submission.contestId,
                        handle = info.handle,
                        context = context
                    )
                }
            }
        }

        return Result.success()
    }

    private suspend inline fun getNewSubmissions(
        handle: String,
        lastSubmissionId: Long?
    ) = buildList {
            var from = 1L
            var step = 1L
            while (true) {
                val items = CodeforcesApi.getUserSubmissions(handle = handle, from = from, count = step)
                    .filter { isActual(it.creationTime) }
                    .filter { lastSubmissionId == null || it.id > lastSubmissionId }

                if (items.isEmpty()) break
                addAll(items)

                from += step
                step += 10
            }
        }
}