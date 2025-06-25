package com.demich.cps.platforms.api.codechef

interface CodeChefApi {
    suspend fun getSuggestions(str: String): CodeChefSearchResult
}