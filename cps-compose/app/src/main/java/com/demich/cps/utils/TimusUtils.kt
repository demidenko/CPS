package com.demich.cps.utils

import io.ktor.client.request.*

object TimusAPI {

    private val client = cpsHttpClient {

    }

    suspend fun getUserPage(id: Int): String {
        return client.get(URLFactory.user(id)) {
            parameter("locale", "en")
        }
    }

    suspend fun getSearchPage(str: String): String {
        return client.get(URLFactory.main + "/search.aspx") {
            parameter("Str", str)
        }
    }

    object URLFactory {
        const val main = "https://timus.online"
        fun user(id: Int) = "$main/author.aspx?id=$id"
    }
}
