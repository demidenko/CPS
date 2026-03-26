package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.isContestant
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.utils.removeOld
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.minOfNotNull
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
                CodeforcesProfileManager().settingsStorage(context).monitorEnabled()

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesMonitorLauncherWorker>(
                    repeatInterval = 1.hours
                )
        }
    }

    private fun isActual(time: Instant) = workerStartTime - time < 24.hours

    override suspend fun runWork(): Result {
        //TODO: restart failed monitor

        val storage = CodeforcesProfileManager().profileStorage(context)

        //TODO: optimize read by snapshot

        with(storage) {
            val info = profile()?.userInfoOrNull() ?: return Result.success()

            val get: GetResult? = CodeforcesClient.getFirstNewSubmissions(
                handle = info.handle,
                lastId = monitorLastSubmissionId(),
                isActual = ::isActual,
                predicate = { it.author.isContestant() }
            )

            get?.firstParticipation?.let { submission ->
                if (monitorCanceledContests().none { it == submission.contestId }) {
                    CodeforcesMonitorWorker.start(
                        contestId = submission.contestId,
                        handle = info.handle,
                        context = context
                    )
                }
            }

            edit {
                get?.firstSubmission?.let { monitorLastSubmissionId.value = it.id }
                monitorCanceledContests.removeOld { !isActual(it) }
            }
        }

        work.enqueueToCodeforcesContest(workerStartTime)

        return Result.success()
    }
}

private class GetResult(
    val firstSubmission: CodeforcesSubmission,
    val firstParticipation: CodeforcesSubmission?
)

private suspend inline fun CodeforcesApi.getFirstNewSubmissions(
    handle: String,
    lastId: Long?,
    isActual: (Instant) -> Boolean,
    predicate: (CodeforcesSubmission) -> Boolean
): GetResult? {
    var first: CodeforcesSubmission? = null
    var from = 1L
    var step = 1L
    loop@while (true) {
        getUserSubmissions(handle = handle, from = from, count = step)
            .also { if (it.isEmpty()) break@loop }
            .forEach {
                if (isActual(it.creationTime) && (lastId == null || it.id > lastId)) {
                    if (first == null) first = it
                    if (predicate(it)) return GetResult(firstParticipation = it, firstSubmission = first)
                } else {
                    break@loop
                }
            }
        from += step
        step += 10
    }

    if (first == null) return null
    return GetResult(firstParticipation = null, firstSubmission = first)
}

private suspend fun CPSPeriodicWork.enqueueToCodeforcesContest(
    workerStartTime: Instant
) {
    context.contestsRepository.getContestsNotFinished(
        platform = Contest.Platform.codeforces,
        currentTime = workerStartTime
    ).minOfNotNull {
        when (it.phaseAt(workerStartTime)) {
            RUNNING if it.duration < 24.hours -> {
                enqueueAsap()
                return
            }
            BEFORE -> it.startTime
            else -> null
        }
    }?.let {
        enqueueAtIfEarlier(time = it + 5.minutes)
    }
}