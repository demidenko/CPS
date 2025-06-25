package com.demich.cps.platforms.api.timus

interface TimusPageContentProvider {
    suspend fun getUserPage(id: Int): String

    suspend fun getSearchPage(str: String): String
}