package com.demich.cps.platforms.api

import io.ktor.client.request.parameter

object TimusApi: PlatformClient {

    suspend fun getUserPage(id: Int): String {
        return client.getText(urlString = TimusUrls.user(id)) {
            parameter("locale", "en")
        }
    }

    suspend fun getSearchPage(str: String): String {
        return client.getText(urlString = "${TimusUrls.main}/search.aspx") {
            parameter("Str", str)
        }
    }
}

object TimusUrls {
    const val main = "https://timus.online"
    fun user(id: Int) = "$main/author.aspx?id=$id"
}