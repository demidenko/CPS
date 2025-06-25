package com.demich.cps.platforms.clients

import com.demich.cps.platforms.api.timus.TimusPageContentProvider
import com.demich.cps.platforms.api.timus.TimusUrls
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

