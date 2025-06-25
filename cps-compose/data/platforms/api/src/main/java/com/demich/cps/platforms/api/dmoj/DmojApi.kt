package com.demich.cps.platforms.api.dmoj

interface DmojApi {
    suspend fun getUser(handle: String): DmojUserResult

    suspend fun getContests(): List<DmojContest>

    suspend fun getSuggestions(str: String): List<DmojSuggestion>
}