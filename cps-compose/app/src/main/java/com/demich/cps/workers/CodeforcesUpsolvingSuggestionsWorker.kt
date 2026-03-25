package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblem
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.problemId
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.getContestAcceptedStatistics
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.utils.add
import com.demich.cps.utils.awaitPair
import com.demich.cps.utils.removeOlderThan
import com.demich.datastore_itemized.edit
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

    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_upsolving", context = context) {
            override suspend fun isEnabled() =
                CodeforcesProfileManager().settingsStorage(context).upsolvingSuggestionsEnabled()

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesUpsolvingSuggestionsWorker>(
                    repeatInterval = 12.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        val storage = CodeforcesProfileManager().profileStorage(context)

        val handle = storage.profile()
            ?.userInfoOrNull()
            ?.takeIf { it.rating != null }
            ?.handle
            ?: return Result.success()

        val dateThreshold = workerStartTime - 90.days

        storage.edit {
            upsolvingSuggestedProblems.removeOlderThan(dateThreshold)
        }

        val suggestedItem = storage.upsolvingSuggestedProblems
        val suggestedProblems = suggestedItem()

        with(CodeforcesClient) {
            getUserRatingChanges(handle)
                .filter { it.ratingUpdateTime >= dateThreshold }
                .sortedByDescending { it.ratingUpdateTime }
                .forEachWithProgress { ratingChange ->
                    getContestUpsolvingSuggestions(
                        contestId = ratingChange.contestId,
                        handle = handle,
                        rank = ratingChange.rank,
                        suggestedProblems = suggestedProblems,
                        toRemoveAsSolved = { solved ->
                            val solvedIds = solved.mapToSet { it.problemId }
                            suggestedItem.update {
                                it.filterValues { it.problemId !in solvedIds }
                            }
                        }
                    ) { problem ->
                        suggestedItem.add(problem, ratingChange.ratingUpdateTime)
                        notifyProblemForUpsolve(problem, context)
                    }
                }
        }

        return Result.success()
    }
}

context(api: CodeforcesApi, pageContentProvider: CodeforcesPageContentProvider)
private suspend inline fun getContestUpsolvingSuggestions(
    contestId: Int,
    handle: String,
    rank: Int,
    suggestedProblems: Collection<CodeforcesProblem>,
    toRemoveAsSolved: (List<CodeforcesProblem>) -> Unit,
    onNewSuggestion: (CodeforcesProblem) -> Unit
) {
    val (userSubmissions, acceptedStats) = awaitPair(
        blockFirst = {
            api.getContestSubmissions(contestId = contestId, handle = handle)
        },
        blockSecond = {
            pageContentProvider.getContestAcceptedStatistics(contestId = contestId)
        }
    )

    makeContestUpsolvingSuggestions(
        rank = rank,
        userSubmissions = userSubmissions,
        acceptedStats = acceptedStats,
        contestSuggested = suggestedProblems.filter { it.contestId == contestId },
        toRemoveAsSolved = toRemoveAsSolved,
        onNewSuggestion = onNewSuggestion,
    )
}

private inline fun makeContestUpsolvingSuggestions(
    rank: Int,
    userSubmissions: List<CodeforcesSubmission>,
    acceptedStats: List<Pair<CodeforcesProblem, Int>>,
    contestSuggested: Collection<CodeforcesProblem>,
    toRemoveAsSolved: (List<CodeforcesProblem>) -> Unit,
    onNewSuggestion: (CodeforcesProblem) -> Unit
) {
    val solvedIndices = userSubmissions
        .filter { it.verdict == OK }
        .mapToSet { it.problem.index }

//    check(acceptedStats.mapToSet { it.first.index }.containsAll(solvedIndices))
    check(solvedIndices.all { index -> acceptedStats.any { it.first.index == index } })

    contestSuggested.filter { it.index in solvedIndices }.let { solved ->
        if (solved.isNotEmpty()) toRemoveAsSolved(solved)
    }

    val contestSuggestedIndices = contestSuggested.mapToSet { it.index }

    acceptedStats.forEach { (problem, solvers) ->
        if (solvers >= rank && problem.index !in solvedIndices) {
            if (problem.index !in contestSuggestedIndices) onNewSuggestion(problem)
        }
    }
}

private fun notifyProblemForUpsolve(problem: CodeforcesProblem, context: Context) {
    val problemId = problem.problemId
    notificationChannels.codeforces.upsolving_suggestion(problemId).notify(context) {
        smallIcon = R.drawable.ic_training
        contentTitle = "Consider to upsolve problem $problemId"
        subText = "codeforces upsolving suggestion"
        time = null
        autoCancel = true
        url = CodeforcesUrls.problem(problem.contestId, problem.index)
    }
}
