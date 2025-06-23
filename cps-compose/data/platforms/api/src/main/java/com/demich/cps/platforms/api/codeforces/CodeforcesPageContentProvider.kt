package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale

interface CodeforcesPageContentProvider {
    enum class BasePage(val path: String) {
        main(""),
        top("top"),
        recent("recent-actions"),
        groups("groups")
    }

    suspend fun getPage(
        page: BasePage,
        locale: CodeforcesLocale
    ): String

    suspend fun getHandleSuggestionsPage(str: String): String

    suspend fun getUserPage(handle: String): String

    suspend fun getContestPage(contestId: Int): String

    suspend fun getTopCommentsPage(
        locale: CodeforcesLocale,
        days: Int = 2
    ): String
}