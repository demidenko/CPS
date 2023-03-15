package com.demich.cps.platforms.api

import io.ktor.client.request.*

object TimusApi: PlatformApi {

    suspend fun getUserPage(id: Int): String {
        return client.getText(urlString = urls.user(id)) {
            parameter("locale", "en")
        }
    }

    suspend fun getSearchPage(str: String): String {
        return client.getText(urlString = "${urls.main}/search.aspx") {
            parameter("Str", str)
        }
    }

    object urls {
        const val main = "https://timus.online"
        fun user(id: Int) = "$main/author.aspx?id=$id"
    }
}