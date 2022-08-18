package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.utils.codeforces.CodeforcesApi
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
    }

    private fun isOld(time: Instant) = currentTime - time > 24.hours

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

            monitorCanceledContests.updateValue { list ->
                list.filter { !isOld(it.second) }
            }
            val canceledContests = monitorCanceledContests().map { it.first }

            newSubmissions.firstOrNull { submission ->
                submission.author.participantType.participatedInContest()
            }?.let { submission ->
                //TODO try start monitor
            }
        }

        return Result.success()
    }

    private suspend fun getNewSubmissions(handle: String, lastSubmissionId: Long?) =
        buildList {
            var from = 1L
            var step = 1L
            while (true) {
                val submissions = CodeforcesApi.getUserSubmissions(
                    handle = handle,
                    from = from,
                    count = step
                )
                var added = false
                for (submission in submissions) {
                    if (isOld(submission.creationTime)) break
                    if (lastSubmissionId != null && submission.id <= lastSubmissionId) break
                    add(submission)
                    added = true
                }
                if (!added) break
                from += step
                step += 10
            }
        }
}