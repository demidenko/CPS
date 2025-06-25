package com.demich.cps.platforms.api.acmp

import com.demich.cps.platforms.api.PlatformClient
import com.demich.cps.platforms.api.cpsHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.Charsets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.net.URLEncoder
import java.nio.charset.Charset

object ACMPClient: PlatformClient, ACMPPageContentProvider {
    private val windows1251 = Charset.forName("windows-1251")
    override val client = cpsHttpClient {
        followRedirects = false

        Charsets {
            register(windows1251)
            responseCharsetFallback = windows1251
        }
    }

    //TODO: ktor can't get charset from client
    private suspend fun HttpResponse.bodyAsText() = bodyAsText(fallbackCharset = windows1251)

    //TODO: this function is copy of top level
    private suspend inline fun HttpClient.getText(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String = this.get(urlString = urlString, block = block).bodyAsText()

    override suspend fun getUserPage(id: Int): String {
        return client.get(ACMPUrls.user(id)).bodyAsText()
    }

    override suspend fun getUsersSearch(str: String): String {
        return client.getText(ACMPUrls.main + "/index.asp") {
            parameter("main", "rating")
            url.encodedParameters.append("str", URLEncoder.encode(str, windows1251.name()))
        }
    }
}

object ACMPUrls {
    const val main = "https://acmp.ru"
    fun user(id: Int) = "$main/index.asp?main=user&id=$id"
}