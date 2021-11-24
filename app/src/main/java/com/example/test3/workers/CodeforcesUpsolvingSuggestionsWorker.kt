package com.example.test3.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.utils.*
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class CodeforcesUpsolvingSuggestionsWorker(private val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val codeforcesAccountManager = CodeforcesAccountManager(context)

        if(codeforcesAccountManager.getSettings().upsolvingSuggestionsEnabled().not()) {
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_upsolving_suggestions)
            return Result.success()
        }

        val handle = codeforcesAccountManager.getSavedInfo().handle

        val deadLine = getCurrentTime() - 90.days
        val ratingChanges = CodeforcesAPI.getUserRatingChanges(handle)
            ?.result
            ?.filter { Instant.fromEpochSeconds(it.ratingUpdateTimeSeconds) > deadLine }
            ?: return Result.failure()

        for (ratingChange in ratingChanges) {
            val contestId = ratingChange.contestId
            val (userSubmissions, problemSolvedBy) = asyncPair(
                { CodeforcesAPI.getContestSubmissions(
                        contestId = contestId,
                        handle = handle
                    )?.result },
                { CodeforcesAPI.getContestProblemsAcceptedsCount(contestId) }
            ).let {
                val first = it.first ?: return Result.failure()
                val second = it.second ?: return Result.failure()
                first to second
            }
            val solvedProblems = userSubmissions
                .filter { it.verdict == CodeforcesProblemVerdict.OK }
                .map { it.problem.index }
                .toSet()
                .onEach { problem ->
                    if(problem !in problemSolvedBy) return Result.failure()
                }
            val suggestedList = codeforcesAccountManager.getSettings().upsolvingSuggestedProblems().toMutableList()
            problemSolvedBy.forEach { (problem, countOfAccepted) ->
                if (problem !in solvedProblems && countOfAccepted >= ratingChange.rank) {
                    val problemContestAndIndex = contestId to problem
                    if (problemContestAndIndex !in suggestedList) {
                        suggestedList.add(problemContestAndIndex)
                        notifyProblemForUpsolve(contestId, problem)
                    }
                }
            }
            codeforcesAccountManager.getSettings().upsolvingSuggestedProblems(suggestedList)
        }

        return Result.success()
    }

    private fun notifyProblemForUpsolve(contestId: Int, problemIndex: String) {
        val problemFullId = "$contestId$problemIndex"
        notificationBuildAndNotify(
            context,
            NotificationChannels.codeforces_upsolving_suggestion,
            NotificationIDs.makeCodeforcesUpsolveProblemID(problemFullId)
        ) {
            setSmallIcon(R.drawable.ic_training)
            setContentTitle("Consider to upsolve problem $problemFullId")
            setSubText("codeforces upsolving suggestion")
            setShowWhen(false)
            setAutoCancel(true)
            setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.problem(contestId, problemIndex), context))
        }
    }

}