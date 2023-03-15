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
        fun getWork(context: Context) = object : CPSWork(name = "cf_monitor_launcher", context = context) {
            override suspend fun isEnabled() = CodeforcesAccountManager(context).getSettings().monitorEnabled()
            override val requestBuilder get() =
                CPSPeriodicWorkRequestBuilder<CodeforcesMonitorLauncherWorker>(
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

            context.workManager.enqueueCodeforcesMonitorWorker(replace)
        }
    }

    private fun isActual(time: Instant) = currentTime - time < 24.hours

    override suspend fun runWork(): Result {
        val manager = CodeforcesAccountManager(context)

        val info = manager.getSavedInfo()
        if (info.status != STATUS.OK) return Result.success()

        val lastSubmissionId = manager.getSettings().monitorLastSubmissionId()

        val newSubmissions = kotlin.runCatching {
            getNewSubmissions(info.handle, lastSubmissionId)
        }.getOrElse {
            return Result.retry()
        }

        manager.getSettings().apply {
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

    private suspend fun getNewSubmissions(handle: String, lastSubmissionId: Long?) =
        buildList {
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