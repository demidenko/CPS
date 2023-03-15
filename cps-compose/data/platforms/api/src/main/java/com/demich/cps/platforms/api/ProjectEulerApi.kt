package com.demich.cps.platforms.api

object ProjectEulerApi: PlatformApi {

    suspend fun getRSSPage(): String {
        return client.getText(urlString = "${urls.main}/rss2_euler.xml")
    }

    suspend fun getRecentPage(): String {
        return client.getText(urlString = "${urls.main}/recent")
    }

    object urls {
        const val main = "https://projecteuler.net"
        val news get() = "$main/news"

        fun problem(id: Int) = "$main/problem=$id"
    }
}