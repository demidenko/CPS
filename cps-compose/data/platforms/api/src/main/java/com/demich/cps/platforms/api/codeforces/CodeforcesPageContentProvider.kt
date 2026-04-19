package com.demich.cps.platforms.api.codeforces

interface CodeforcesPageContentProvider {
    suspend fun getHandleSuggestionsPage(str: String): String

    suspend fun getUserPage(handle: String): String

    suspend fun getContestPage(contestId: Int): String

    suspend fun getMainPage(): String

    suspend fun getRecentActionsPage(): String

    suspend fun getTopBlogEntriesPage(): String

    suspend fun getTopCommentsPage(days: Int = 2): String

    suspend fun getGroupsPage(): String
}