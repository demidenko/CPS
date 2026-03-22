package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestRatingChangesUnavailableException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange

suspend fun CodeforcesApi.getContestRatingChangesStatus(
    contestId: Int
): CodeforcesRatingChangesStatus {
    val ratingChanges = try {
        getContestRatingChanges(contestId = contestId)
    } catch (_: CodeforcesApiContestRatingChangesUnavailableException) {
        return CodeforcesRatingChangesStatus.Unavailable
    }

    if (ratingChanges.isEmpty()) {
        return CodeforcesRatingChangesStatus.Empty
    }

    return CodeforcesRatingChangesStatus.Done(ratingChanges)
}

sealed interface CodeforcesRatingChangesStatus {
    data object Unavailable: CodeforcesRatingChangesStatus
    data object Empty: CodeforcesRatingChangesStatus
    data class Done(val ratingChanges: List<CodeforcesRatingChange>): CodeforcesRatingChangesStatus
}