package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.utils.awaitPair
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesProblem
import com.demich.cps.utils.codeforces.CodeforcesProblemVerdict
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.datastore_itemized.edit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


class CodeforcesUpsolvingSuggestionsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {

    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_upsolving", context = context) {
            override suspend fun isEnabled() = CodeforcesAccountManager(context).getSettings().upsolvingSuggestionsEnabled()
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesUpsolvingSuggestionsWorker>(
                    repeatInterval = 12.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        //TODO: clean old suggestions
        val codeforcesAccountManager = CodeforcesAccountManager(context)
        val handle = codeforcesAccountManager.getSavedInfo()
            .takeIf { it.hasRating() }
            ?.handle
            ?: return Result.success()

        val deadLine = currentTime - 90.days

        val ratingChanges = CodeforcesApi.runCatching {
            getUserRatingChanges(handle)
        }.getOrNull() ?: return Result.retry()

        ratingChanges
            .filter { it.ratingUpdateTime > deadLine }
            .forEachWithProgress { ratingChange ->
                val contestId = ratingChange.contestId
                val data = awaitPair(
                    blockFirst = {
                        CodeforcesApi.runCatching {
                            getContestSubmissions(
                                contestId = contestId,
                                handle = handle
                            )
                        }.getOrNull()
                    },
                    blockSecond = {
                        CodeforcesUtils.getContestAcceptedStatistics(contestId)
                    }
                )

                val userSubmissions = data.first ?: return Result.retry()
                val acceptedStats = data.second ?: return Result.failure()

                val solvedProblems = userSubmissions
                    .filter { it.verdict == CodeforcesProblemVerdict.OK }
                    .map { it.problem.index }

                if (!acceptedStats.map { it.key.index }.containsAll(solvedProblems)) {
                    return Result.failure()
                }

                acceptedStats.forEach { (problem, countOfAccepted) ->
                    if (problem.index !in solvedProblems && countOfAccepted >= ratingChange.rank) {
                        codeforcesAccountManager.getSettings().upsolvingSuggestedProblems.edit {
                            if (none { it.contestId == contestId && it.index == problem.index }) {
                                add(problem)
                                notifyProblemForUpsolve(problem)
                            }
                        }
                    }
                }
            }

        return Result.success()
    }

    private fun notifyProblemForUpsolve(problem: CodeforcesProblem) {
        val problemId = "${problem.contestId}${problem.index}"
        notificationBuildAndNotify(
            context,
            NotificationChannels.codeforces.upsolving_suggestion,
            NotificationIds.makeCodeforcesUpsolveProblemId(problemId)
        ) {
            setSmallIcon(R.drawable.ic_training)
            setContentTitle("Consider to upsolve problem $problemId")
            setSubText("codeforces upsolving suggestion")
            setShowWhen(false)
            setAutoCancel(true)
            attachUrl(url = CodeforcesApi.urls.problem(problem.contestId, problem.index), context = context)
        }
    }

}