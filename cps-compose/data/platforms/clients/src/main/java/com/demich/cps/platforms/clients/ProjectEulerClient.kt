package com.demich.cps.platforms.clients

import com.demich.cps.platforms.api.projecteuler.ProjectEulerPageContentProvider
import com.demich.cps.platforms.api.projecteuler.ProjectEulerUrls

object ProjectEulerClient: PlatformClient, ProjectEulerPageContentProvider {

    private suspend fun getPage(page: String): String {
        return client.getText(urlString = "${ProjectEulerUrls.main}/$page")
    }

    override suspend fun getNewsPage(): String {
        return getPage(page = "news")
    }

    override suspend fun getRSSPage(): String {
        return getPage(page = "rss2_euler.xml")
    }

    override suspend fun getRecentPage(): String {
        return getPage(page = "recent")
    }
}

