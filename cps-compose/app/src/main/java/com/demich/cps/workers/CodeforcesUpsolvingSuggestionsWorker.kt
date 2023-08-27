package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.utils.awaitPair
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesProblem
import com.demich.cps.platforms.api.CodeforcesProblemVerdict
import com.demich.cps.utils.mapToSet
import com.demich.datastore_itemized.add
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
            override suspend fun isEnabled() =
                CodeforcesAccountManager().getSettings(context).upsolvingSuggestionsEnabled()

            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesUpsolvingSuggestionsWorker>(
                    repeatInterval = 12.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        val dataStore = CodeforcesAccountManager().dataStore(context)

        val handle = dataStore.getSavedInfo()
            ?.takeIf { it.hasRating() }
            ?.handle
            ?: return Result.success()

        val suggestedItem = dataStore.upsolvingSuggestedProblems

        val deadLine = currentTime - 90.days
        suggestedItem.edit { filter { it.second > deadLine } }

        val ratingChanges = CodeforcesApi.runCatching {
            getUserRatingChanges(handle)
        }.getOrNull() ?: return Result.retry()

        val alreadySuggested = suggestedItem().map { it.first.problemId }

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

                val solvedIndices = userSubmissions
                    .filter { it.verdict == CodeforcesProblemVerdict.OK }
                    .mapToSet { it.problem.index }

                require(acceptedStats.map { it.key.index }.containsAll(solvedIndices))

                //remove solved problems from suggestions list
                suggestedItem.edit {
                    removeAll { it.first.contestId == contestId && it.first.index in solvedIndices }
                }

                //add new suggestions
                acceptedStats.forEach { (problem, solvers) ->
                    if (solvers >= ratingChange.rank && problem.index !in solvedIndices) {
                        if (problem.problemId !in alreadySuggested) {
                            suggestedItem.add(problem to ratingChange.ratingUpdateTime)
                            notifyProblemForUpsolve(problem, context)
                        }
                    }
                }
            }

        return Result.success()
    }

}

private fun notifyProblemForUpsolve(problem: CodeforcesProblem, context: Context) {
    val problemId = problem.problemId
    notificationChannels.codeforces.upsolving_suggestion(problemId).notify(context) {
        setSmallIcon(R.drawable.ic_training)
        setContentTitle("Consider to upsolve problem $problemId")
        setSubText("codeforces upsolving suggestion")
        setShowWhen(false)
        setAutoCancel(true)
        attachUrl(url = CodeforcesApi.urls.problem(problem.contestId, problem.index), context = context)
    }
}
