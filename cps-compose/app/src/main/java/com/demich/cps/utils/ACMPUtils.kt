package com.demich.cps.utils

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder
import java.nio.charset.Charset

object ACMPApi {
    private val windows1251 = Charset.forName("windows-1251")
    private val client = cpsHttpClient(json = false) {
        Charsets {
            register(windows1251)
            responseCharsetFallback = windows1251
        }
    }

    class ACMPPageNotFoundException : Throwable()

    suspend fun getMainPage(): String {
        return client.get(urls.main)
    }

    suspend fun getUserPage(id: Int): String {
        val response = client.get<HttpResponse>(urls.user(id))
        with(response.call) {
            //acmp redirects to main page if user not found
            if (request.url.parameters.isEmpty()) throw ACMPPageNotFoundException()
            return receive()
        }
    }

    suspend fun getUsersSearch(str: String): String {
        return client.get(urls.main + "/index.asp?main=rating") {
            url {
                parameters.urlEncodingOption = UrlEncodingOption.KEY_ONLY
                parameters.append("str", URLEncoder.encode(str, windows1251.name()))
            }
        }
    }

    object urls {
        const val main = "https://acmp.ru"
        fun user(id: Int) = "$main/index.asp?main=user&id=$id"
    }
}
