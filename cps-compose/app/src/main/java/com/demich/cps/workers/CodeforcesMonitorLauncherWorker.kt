package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.utils.removeOld
import com.demich.kotlin_stdlib_boost.minOfNotNull
import kotlinx.datetime.toStdlibInstant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class CodeforcesMonitorLauncherWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_monitor_launcher", context = context) {
            override suspend fun isEnabled() =
                CodeforcesAccountManager().getSettings(context).monitorEnabled()

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesMonitorLauncherWorker>(
                    repeatInterval = 1.hours
                )
        }
    }

    private fun isActual(time: Instant) = workerStartTime.toStdlibInstant() - time < 24.hours

    override suspend fun runWork(): Result {
        //TODO: restart failed monitor

        val dataStore = CodeforcesAccountManager().dataStore(context)

        val info = dataStore.getProfile()?.userInfoOrNull() ?: return Result.success()

        with(dataStore) {
            val (firstParticipation, firstSubmission) = getFirstNewSubmissions(
                handle = info.handle,
                lastSubmissionId = monitorLastSubmissionId()
            ) { submission ->
                submission.author.isContestParticipant()
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
                monitorLastSubmissionId.setValue(it.id)
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
        loop@while (true) {
            CodeforcesClient.getUserSubmissions(handle = handle, from = from, count = step)
                .also { if (it.isEmpty()) break@loop }
                .forEach {
                    if (isActual(it.creationTime) && (lastSubmissionId == null || it.id > lastSubmissionId)) {
                        if (first == null) first = it
                        if (predicate(it)) return Pair(it, first)
                    } else {
                        break@loop
                    }
                }

            from += step
            step += 10
        }
        return Pair(null, first)
    }

    private suspend fun enqueueToCodeforcesContest() {
        context.contestsListDao.getContestsNotFinished(
            platform = Contest.Platform.codeforces,
            currentTime = workerStartTime
        ).minOfNotNull {
            when (it.getPhase(workerStartTime)) {
                Contest.Phase.RUNNING -> {
                    work.enqueueAsap()
                    return
                }
                Contest.Phase.BEFORE -> it.startTime
                else -> null
            }
        }?.let {
            work.enqueueAtIfEarlier(time = it.toStdlibInstant() + 5.minutes)
        }
    }
}