package com.demich.cps.utils

import io.ktor.client.request.*

object TimusApi {
    private val client = cpsHttpClient(json = false) { }

    suspend fun getUserPage(id: Int): String {
        return client.get(urls.user(id)) {
            parameter("locale", "en")
        }
    }

    suspend fun getSearchPage(str: String): String {
        return client.get(urls.main + "/search.aspx") {
            parameter("Str", str)
        }
    }

    object urls {
        const val main = "https://timus.online"
        fun user(id: Int) = "$main/author.aspx?id=$id"
    }
}
