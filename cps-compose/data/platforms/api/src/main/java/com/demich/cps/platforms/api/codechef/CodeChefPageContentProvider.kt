package com.demich.cps.platforms.api.codechef

interface CodeChefPageContentProvider {
    suspend fun getUserPage(handle: String): String

    suspend fun getRatingChanges(handle: String): List<CodeChefRatingChange>
}