package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestRatingChangesUnavailableException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange

// experimental
suspend fun CodeforcesApi.getContestRatingChangesStatus(
    contestId: Int
): CodeforcesRatingChangesStatus {
    val ratingChanges = try {
        getContestRatingChanges(contestId = contestId)
    } catch (_: CodeforcesApiContestRatingChangesUnavailableException) {
        return CodeforcesRatingChangesStatus.Unrated
    }

    if (ratingChanges.isEmpty()) {
        // not perfect, for some contest it is unrated (ex. 1726)
        return CodeforcesRatingChangesStatus.Pending
    }

    return CodeforcesRatingChangesStatus.Done(ratingChanges)
}

sealed interface CodeforcesRatingChangesStatus {
    data object Unrated: CodeforcesRatingChangesStatus
    data object Pending: CodeforcesRatingChangesStatus
    data class Done(val ratingChanges: List<CodeforcesRatingChange>): CodeforcesRatingChangesStatus
}