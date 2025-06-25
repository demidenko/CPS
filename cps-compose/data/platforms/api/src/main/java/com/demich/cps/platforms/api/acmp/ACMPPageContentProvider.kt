package com.demich.cps.platforms.api.acmp

interface ACMPPageContentProvider {
    suspend fun getUserPage(id: Int): String

    suspend fun getUsersSearch(str: String): String
}