package com.demich.cps.platforms.api

object ProjectEulerApi: PlatformClient {

    private suspend fun getPage(page: String): String {
        return client.getText(urlString = "${ProjectEulerUrls.main}/$page")
    }

    suspend fun getNewsPage(): String {
        return getPage(page = "news")
    }

    suspend fun getRSSPage(): String {
        return getPage(page = "rss2_euler.xml")
    }

    suspend fun getRecentPage(): String {
        return getPage(page = "recent")
    }
}

object ProjectEulerUrls {
    const val main = "https://projecteuler.net"
    val news get() = "$main/news"

    fun problem(id: Int) = "$main/problem=$id"
}