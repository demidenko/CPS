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
    suspend fun getContests(
        gym: Boolean? = null
    ): List<CodeforcesContest>

    //TODO: Sequence instead of List
    suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange>

    suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>? = null,
        showUnofficial: Boolean? = null,
        participantTypes: Collection<CodeforcesParticipationType>? = null
    ): CodeforcesContestStandings

    suspend fun getContestSubmissions(
        contestId: Int,
        handle: String? = null,
        from: Int? = null,
        count: Int? = null
    ): List<CodeforcesSubmission>

    suspend fun getRecentActions(maxCount: Int): List<CodeforcesRecentAction>

    suspend fun getUserBlogEntries(handle: String): List<CodeforcesBlogEntry>

    suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean = true
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
    participantTypes: Collection<CodeforcesParticipationType>
): CodeforcesContestStandings =
    getContestStandings(
        contestId = contestId,
        handles = listOf(handle),
        participantTypes = participantTypes
    )

suspend fun CodeforcesApi.getUser(
    handle: String,
    checkHistoricHandles: Boolean
): CodeforcesUser =
    getUsers(
        handles = listOf(handle),
        checkHistoricHandles = checkHistoricHandles
    ).single()

suspend fun CodeforcesApi.getRecentActions() = getRecentActions(maxCount = 100)

data class CodeforcesApiAccess(
    val key: String,
    val secret: String
)