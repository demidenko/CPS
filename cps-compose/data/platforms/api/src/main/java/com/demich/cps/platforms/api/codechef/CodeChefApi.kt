package com.demich.cps.platforms.api.codechef

interface CodeChefApi {
    suspend fun getUserSuggestions(str: String): List<CodeChefUser>
}