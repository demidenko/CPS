package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale

interface CodeforcesPageContentProvider {
    suspend fun getHandleSuggestionsPage(str: String): String

    suspend fun getUserPage(handle: String): String

    suspend fun getContestPage(contestId: Int): String

    suspend fun getMainPage(locale: CodeforcesLocale): String

    suspend fun getRecentActionsPage(locale: CodeforcesLocale): String

    suspend fun getTopBlogEntriesPage(locale: CodeforcesLocale): String

    suspend fun getTopCommentsPage(
        locale: CodeforcesLocale,
        days: Int = 2
    ): String

    suspend fun getGroupsPage(locale: CodeforcesLocale): String
}