package com.demich.cps.platforms.api.clist

interface ClistPageContentProvider {
    suspend fun getUserPage(login: String): String

    suspend fun getUsersSearchPage(str: String): String
}