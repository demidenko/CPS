package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesProblem
import com.demich.cps.platforms.api.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.CodeforcesRatingChange
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.add
import com.demich.cps.utils.awaitPair
import com.demich.kotlin_stdlib_boost.mapToSet
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
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_upsolving", context = context) {
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

        val dateThreshold = workerStartTime - 90.days
        suggestedItem.update { it.withoutOld(dateThreshold) }

        val ratingChanges = CodeforcesApi.getUserRatingChanges(handle)

        val alreadySuggested = suggestedItem()

        ratingChanges
            .filter { it.ratingUpdateTime >= dateThreshold }
            .sortedByDescending { it.ratingUpdateTime }
            .forEachWithProgress { ratingChange ->
                getSuggestions(
                    handle = handle,
                    ratingChange = ratingChange,
                    alreadySuggested = alreadySuggested,
                    toRemoveAsSolved = { solved ->
                        val solvedIds = solved.mapToSet { it.problemId }
                        suggestedItem.update {
                            it.without { it.problemId in solvedIds }
                        }
                    }
                ) { problem ->
                    suggestedItem.add(problem, ratingChange.ratingUpdateTime)
                    notifyProblemForUpsolve(problem, context)
                }
            }

        return Result.success()
    }
}

private suspend inline fun getSuggestions(
    handle: String,
    ratingChange: CodeforcesRatingChange,
    alreadySuggested: Collection<CodeforcesProblem>,
    toRemoveAsSolved: (List<CodeforcesProblem>) -> Unit,
    onNewSuggestion: (CodeforcesProblem) -> Unit
) {
    val contestId = ratingChange.contestId
    val (userSubmissions, acceptedStats) = awaitPair(
        blockFirst = {
            CodeforcesApi.getContestSubmissions(contestId = contestId, handle = handle)
        },
        blockSecond = {
            CodeforcesUtils.extractContestAcceptedStatistics(
                source = CodeforcesApi.getContestPage(contestId = contestId),
                contestId = contestId
            )
        }
    )

    val solvedIndices = userSubmissions
        .filter { it.verdict == CodeforcesProblemVerdict.OK }
        .mapToSet { it.problem.index }

    require(acceptedStats.map { it.key.index }.containsAll(solvedIndices))

    alreadySuggested.filter { it.contestId == contestId && it.index in solvedIndices }.let { solved ->
        if (solved.isNotEmpty()) toRemoveAsSolved(solved)
    }

    val alreadySuggestedIndices = alreadySuggested
        .filter { it.contestId == contestId }
        .mapToSet { it.index }

    acceptedStats.forEach { (problem, solvers) ->
        if (solvers >= ratingChange.rank && problem.index !in solvedIndices) {
            if (problem.index !in alreadySuggestedIndices) onNewSuggestion(problem)
        }
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
