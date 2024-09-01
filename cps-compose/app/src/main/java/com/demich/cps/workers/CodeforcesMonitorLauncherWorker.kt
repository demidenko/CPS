package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
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

        val newSubmissions = getNewSubmissions(
            handle = info.handle,
            lastSubmissionId = dataStore.monitorLastSubmissionId()
        )

        with(dataStore) {
            newSubmissions.firstOrNull { submission ->
                submission.author.participantType.contestParticipant()
            }?.let { submission ->
                if (monitorCanceledContests().none { it == submission.contestId }) {
                    CodeforcesMonitorWorker.start(
                        contestId = submission.contestId,
                        handle = info.handle,
                        context = context
                    )
                }
            }

            newSubmissions.firstOrNull()?.let {
                monitorLastSubmissionId(it.id)
            }

            monitorCanceledContests.update {
                it.withoutOld { time -> !isActual(time) }
            }
        }

        enqueueToCodeforcesContest()

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

    private suspend fun enqueueToCodeforcesContest() {
        with(context.contestsListDao.getContests(Contest.Platform.codeforces)) {
            if (any { it.getPhase(workerStartTime) == Contest.Phase.RUNNING }) {
                work.enqueueAsap()
            }

            filter { it.getPhase(workerStartTime) == Contest.Phase.BEFORE }
                .minOfOrNull { it.startTime }
                ?.let {
                    work.enqueueAt(time = it + 5.minutes, repeatInterval)
                }
        }
    }
}