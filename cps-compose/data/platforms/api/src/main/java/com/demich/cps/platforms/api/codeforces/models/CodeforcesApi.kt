package com.demich.cps.platforms.api.codeforces.models

interface CodeforcesApi {
    // api methods from https://codeforces.com/apiHelp/methods

    suspend fun getBlogEntry(
        blogEntryId: Int,
        locale: CodeforcesLocale
    ): CodeforcesBlogEntry

    //TODO: Sequence instead of List?
    suspend fun getContests(): List<CodeforcesContest>

    //TODO: Sequence instead of List
    suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange>

    suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        includeUnofficial: Boolean
        //TODO: participantTypes: Collection<CodeforcesParticipationType>
    ): CodeforcesContestStandings

    suspend fun getContestStandings(
        contestId: Int,
        handle: String,
        includeUnofficial: Boolean
    ): CodeforcesContestStandings {
        return getContestStandings(contestId, listOf(handle), includeUnofficial)
    }

    suspend fun getContestSubmissions(
        contestId: Int,
        handle: String
    ): List<CodeforcesSubmission>

    suspend fun getRecentActions(
        locale: CodeforcesLocale,
        maxCount: Int = Int.MAX_VALUE
    ): List<CodeforcesRecentAction>

    suspend fun getUserBlogEntries(
        handle: String,
        locale: CodeforcesLocale
    ): List<CodeforcesBlogEntry>

    suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean = false
    ): List<CodeforcesUser>

    suspend fun getUser(
        handle: String,
        checkHistoricHandles: Boolean = false
    ): CodeforcesUser {
        return getUsers(listOf(handle), checkHistoricHandles).first()
    }

    suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange>

    suspend fun getUserSubmissions(
        handle: String,
        count: Long,
        from: Long
    ): List<CodeforcesSubmission>
}