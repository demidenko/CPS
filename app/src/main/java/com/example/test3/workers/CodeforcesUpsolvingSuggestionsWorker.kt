package com.example.test3.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesProblemVerdict
import com.example.test3.utils.asyncPair
import com.example.test3.utils.getCurrentTimeSeconds
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
            problemSolvedBy.forEach { (problem, countOfAccepted) ->
                if (problem !in solvedProblems && countOfAccepted >= ratingChange.rank) {
                    //TODO: consider to upsolve
                    println("consider to upsolve: $contestId $problem (${ratingChange.rank} vs $countOfAccepted)")
                    codeforcesAccountManager.getSettings()
                }
            }
        }


        return Result.success()
    }

}