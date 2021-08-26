package com.example.test3.workers

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.utils.*
import java.util.concurrent.TimeUnit

class CodeforcesUpsolvingSuggestionsWorker(private val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val codeforcesAccountManager = CodeforcesAccountManager(context)

        if(codeforcesAccountManager.getSettings().upsolvingSuggestionsEnabled().not()) {
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_upsolving_suggestions)
            return Result.success()
        }

        val handle = codeforcesAccountManager.getSavedInfo().handle

        val deadLineTimeSeconds = getCurrentTimeSeconds() - TimeUnit.DAYS.toSeconds(90)
        val ratingChanges = CodeforcesAPI.getUserRatingChanges(handle)
            ?.result
            ?.filter { it.ratingUpdateTimeSeconds > deadLineTimeSeconds }
            ?: return Result.failure()

        for (ratingChange in ratingChanges) {
            val contestId = ratingChange.contestId
            val (userSubmissions, problemSolvedBy) = asyncPair(
                {
                    CodeforcesAPI.getContestSubmissions(
                        contestId = contestId,
                        handle = handle
                    )?.result
                },
                {
                    CodeforcesAPI.getContestProblemsAcceptedsCount(contestId)
                }
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
                    if (problemSolvedBy.containsKey(problem).not()) return Result.failure()
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
        val problemFullName = "$contestId$problemIndex"
        val n = notificationBuilder(context, NotificationChannels.codeforces_upsolving_suggestion).apply {
            setSmallIcon(R.drawable.ic_logo_codeforces)
            setContentTitle("Consider to upsolve $problemFullName")
            setSubText("codeforces upsolving suggestion")
            setShowWhen(false)
            setContentIntent(
                makePendingIntentOpenURL(
                    CodeforcesURLFactory.problem(contestId, problemIndex),
                    context
                )
            )
        }
        NotificationManagerCompat.from(context).notify(
            NotificationIDs.makeCodeforcesUpsolveProblemID(problemFullName),
            n.build()
        )
    }

}