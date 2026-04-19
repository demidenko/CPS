package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.isContestant
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.utils.removeOld
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.flowOf
import com.demich.datastore_itemized.fromSnapshot
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

            override fun flowOfInfo() =
                CodeforcesProfileManager().profileStorage(context).flowOf {
                    mapOf(
                        "last submission id" to monitorLastSubmissionId.value,
                        "canceled" to monitorCanceledContests.value.valuesSortedByTime()
                    )
                }
        }
    }

    private fun isActual(time: Instant) = workerStartTime - time < 24.hours

    override suspend fun runWork(): Result {
        //TODO: restart failed monitor

        val storage = CodeforcesProfileManager().profileStorage(context)

        with(storage) {
            val handle: String

            val get: GetResult? = fromSnapshot {
                handle = profile.value?.userInfoOrNull()?.handle ?: return Result.success()
                CodeforcesClient().getFirstNewSubmissions(
                    handle = handle,
                    untilId = monitorLastSubmissionId.value,
                    isActual = ::isActual,
                    predicate = { it.author.isContestant() }
                )
            }

            get?.apply {
                firstPredicate?.let { submission ->
                    if (monitorCanceledContests().none { it == submission.contestId }) {
                        CodeforcesMonitorWorker.start(
                            contestId = submission.contestId,
                            handle = handle,
                            context = context
                        )
                    }
                }

                // note: intentionally run removeOld not every time
                edit {
                    monitorLastSubmissionId.value = first.id
                    monitorCanceledContests.removeOld { !isActual(it) }
                }
            }
        }

        work.enqueueToCodeforcesContest(workerStartTime)

        return Result.success()
    }
}

private class GetResult(
    val first: CodeforcesSubmission,
    val firstPredicate: CodeforcesSubmission?
)

private suspend inline fun CodeforcesApi.getFirstNewSubmissions(
    handle: String,
    untilId: Long?,
    isActual: (Instant) -> Boolean,
    predicate: (CodeforcesSubmission) -> Boolean
): GetResult? {
    var first: CodeforcesSubmission? = null
    var from = 1L
    var count = 1L
    loop@while (true) {
        val submissions = getUserSubmissions(handle = handle, from = from, count = count)
        submissions.forEach {
            if (untilId == null || it.id > untilId) {
                if (first == null) first = it
                if (isActual(it.creationTime)) {
                    if (predicate(it)) return GetResult(firstPredicate = it, first = first)
                } else {
                    break@loop
                }
            } else {
                break@loop
            }
        }
        if (submissions.size < count) break
        from += count
        count += 10
    }

    if (first == null) return null
    return GetResult(firstPredicate = null, first = first)
}

private suspend fun CPSPeriodicWork.enqueueToCodeforcesContest(
    workerStartTime: Instant
) {
    context.contestsRepository.getContestsNotFinished(
        platform = codeforces,
        currentTime = workerStartTime
    ).minOfNotNull {
        when (it.phaseAt(workerStartTime)) {
            RUNNING if it.duration < 24.hours -> {
                enqueueAsap()
                return
            }
            UPCOMING -> it.startTime
            else -> null
        }
    }?.let {
        enqueueAtIfEarlier(time = it + 5.minutes)
    }
}