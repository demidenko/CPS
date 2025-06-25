package com.demich.cps.platforms.api.projecteuler

interface ProjectEulerPageContentProvider {
    suspend fun getNewsPage(): String

    suspend fun getRSSPage(): String

    suspend fun getRecentPage(): String
}