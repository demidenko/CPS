package com.demich.cps.utils

object ProjectEulerApi {
    private val client = cpsHttpClient {  }

    suspend fun getRSSPage(): String {
        return client.getText(urlString = "${urls.main}/rss2_euler.xml")
    }

    object urls {
        const val main = "https://projecteuler.net"
        val news get() = "$main/news"
    }
}