package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestStandings
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser

interface CodeforcesApi {
    // api methods from https://codeforces.com/apiHelp/methods

    suspend fun getBlogEntry(blogEntryId: Int): CodeforcesBlogEntry

    //TODO: Sequence instead of List?
    suspend fun getContests(): List<CodeforcesContest>

    //TODO: Sequence instead of List
    suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange>

    suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        includeUnofficial: Boolean
    ): CodeforcesContestStandings

    suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        participantTypes: Collection<CodeforcesParticipationType>
    ): CodeforcesContestStandings

    suspend fun getContestSubmissions(
        contestId: Int,
        handle: String,
        from: Int,
        count: Int
    ): List<CodeforcesSubmission>

    suspend fun getRecentActions(maxCount: Int): List<CodeforcesRecentAction>

    suspend fun getUserBlogEntries(handle: String): List<CodeforcesBlogEntry>

    suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean = false
    ): List<CodeforcesUser>

    suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange>

    suspend fun getUserSubmissions(
        handle: String,
        from: Long,
        count: Long
    ): List<CodeforcesSubmission>
}

suspend fun CodeforcesApi.getContestStandings(
    contestId: Int,
    handle: String,
    includeUnofficial: Boolean
): CodeforcesContestStandings =
    getContestStandings(
        contestId = contestId,
        handles = listOf(handle),
        includeUnofficial = includeUnofficial
    )

suspend fun CodeforcesApi.getContestStandings(
    contestId: Int,
    handle: String,
    participantTypes: Collection<CodeforcesParticipationType>
): CodeforcesContestStandings =
    getContestStandings(
        contestId = contestId,
        handles = listOf(handle),
        participantTypes = participantTypes
    )

suspend fun CodeforcesApi.getUser(
    handle: String,
    checkHistoricHandles: Boolean = false
): CodeforcesUser =
    getUsers(
        handles = listOf(handle),
        checkHistoricHandles = checkHistoricHandles
    ).first()

suspend fun CodeforcesApi.getContestSubmissions(
    contestId: Int,
    handle: String
): List<CodeforcesSubmission> =
    getContestSubmissions(
        contestId = contestId,
        handle = handle,
        from = 1,
        count = 1e9.toInt()
    )

suspend fun CodeforcesApi.getRecentActions() = getRecentActions(maxCount = 100)

data class CodeforcesApiAccess(
    val key: String,
    val secret: String
)