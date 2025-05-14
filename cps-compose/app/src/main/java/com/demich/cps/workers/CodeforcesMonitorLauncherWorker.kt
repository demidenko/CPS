package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.utils.removeOld
import com.demich.kotlin_stdlib_boost.minOfNotNull
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
        private val repeatInterval get() = 45.minutes
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_monitor_launcher", context = context) {
            override suspend fun isEnabled() =
                CodeforcesAccountManager().getSettings(context).monitorEnabled()

            override val requestBuilder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesMonitorLauncherWorker>(
                    repeatInterval = repeatInterval
                )
        }
    }

    private fun isActual(time: Instant) = workerStartTime - time < 24.hours

    override suspend fun runWork(): Result {
        //TODO: restart failed monitor

        val dataStore = CodeforcesAccountManager().dataStore(context)

        val info = dataStore.getSavedInfo() ?: return Result.success()
        if (info.status != STATUS.OK) return Result.success()

        with(dataStore) {
            val (firstParticipation, firstSubmission) = getFirstNewSubmissions(
                handle = info.handle,
                lastSubmissionId = monitorLastSubmissionId()
            ) { submission ->
                submission.author.participantType.contestParticipant()
            }

            firstParticipation?.let { submission ->
                if (monitorCanceledContests().none { it == submission.contestId }) {
                    CodeforcesMonitorWorker.start(
                        contestId = submission.contestId,
                        handle = info.handle,
                        context = context
                    )
                }
            }

            firstSubmission?.let {
                monitorLastSubmissionId(it.id)
            }

            monitorCanceledContests.removeOld { !isActual(it) }
        }

        enqueueToCodeforcesContest()

        return Result.success()
    }

    private suspend inline fun getFirstNewSubmissions(
        handle: String,
        lastSubmissionId: Long?,
        predicate: (CodeforcesSubmission) -> Boolean
    ): Pair<CodeforcesSubmission?, CodeforcesSubmission?> {
        var first: CodeforcesSubmission? = null
        var from = 1L
        var step = 1L
        while (true) {
            var something = false
            CodeforcesApi.getUserSubmissions(handle = handle, from = from, count = step)
                .forEach {
                    if (isActual(it.creationTime) && (lastSubmissionId == null || it.id > lastSubmissionId)) {
                        something = true
                        if (first == null) first = it
                        if (predicate(it)) return Pair(it, first)
                    } else {
                        // TODO: break on false (also check empty list)
                    }
                }

            if (!something) break

            from += step
            step += 10
        }
        return Pair(null, first)
    }

    private suspend fun enqueueToCodeforcesContest() {
        with(context.contestsListDao.getContests(Contest.Platform.codeforces)) {
            minOfNotNull {
                when (it.getPhase(workerStartTime)) {
                    Contest.Phase.RUNNING -> {
                        work.enqueueAsap()
                        return
                    }
                    Contest.Phase.BEFORE -> it.startTime
                    else -> null
                }
            }?.let {
                work.enqueueAt(time = it + 5.minutes, repeatInterval)
            }
        }
    }
}