package com.demich.cps.platforms.api.dmoj

interface DmojPageContentProvider {
    suspend fun getUserPage(handle: String): String

    suspend fun getRatingChanges(handle: String): List<DmojRatingChange>
}