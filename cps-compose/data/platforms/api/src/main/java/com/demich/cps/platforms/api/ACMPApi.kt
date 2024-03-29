package com.demich.cps.platforms.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.net.URLEncoder
import java.nio.charset.Charset

object ACMPApi: PlatformApi {
    private val windows1251 = Charset.forName("windows-1251")
    override val client = cpsHttpClient {
        Charsets {
            register(windows1251)
            responseCharsetFallback = windows1251
        }
    }

    class ACMPPageNotFoundException : Throwable()

    //TODO: ktor can't get charset from client
    private suspend fun HttpResponse.bodyAsText() = bodyAsText(fallbackCharset = windows1251)

    //TODO: this function is copy of top level
    private suspend inline fun HttpClient.getText(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String = this.get(urlString = urlString, block = block).bodyAsText()

    suspend fun getMainPage(): String {
        return client.getText(urls.main)
    }

    suspend fun getUserPage(id: Int): String {
        with(client.get(urls.user(id))) {
            //acmp redirects to main page if user not found
            if (request.url.parameters.isEmpty()) throw ACMPPageNotFoundException()
            return bodyAsText()
        }
    }

    suspend fun getUsersSearch(str: String): String {
        return client.getText(urls.main + "/index.asp?main=rating") {
            url.encodedParameters.append("str", URLEncoder.encode(str, windows1251.name()))
        }
    }

    object urls {
        const val main = "https://acmp.ru"
        fun user(id: Int) = "$main/index.asp?main=user&id=$id"
    }
}