package com.demich.cps.platforms.api.timus

import com.demich.cps.platforms.api.PlatformClient
import com.demich.cps.platforms.api.getText
import io.ktor.client.request.parameter

object TimusClient: PlatformClient, TimusPageContentProvider {

    override suspend fun getUserPage(id: Int): String {
        return client.getText(urlString = TimusUrls.user(id)) {
            parameter("locale", "en")
        }
    }

    override suspend fun getSearchPage(str: String): String {
        return client.getText(urlString = "${TimusUrls.main}/search.aspx") {
            parameter("Str", str)
        }
    }
}

object TimusUrls {
    const val main = "https://timus.online"
    fun user(id: Int) = "$main/author.aspx?id=$id"
}