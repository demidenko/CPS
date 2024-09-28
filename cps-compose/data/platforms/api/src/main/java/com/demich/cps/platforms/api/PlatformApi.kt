package com.demich.cps.platforms.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface PlatformApi {
    val client: HttpClient get() = defaultHttpClient
}

internal fun cpsHttpClient(
    json: Json? = null,
    useCookies: Boolean = false,
    connectionTimeout: Duration = 15.seconds,
    requestTimeout: Duration = 30.seconds,
    block: HttpClientConfig<*>.() -> Unit
) = HttpClient(OkHttp) {
    expectSuccess = true

    install(HttpTimeout) {
        connectTimeoutMillis = connectionTimeout.inWholeMilliseconds
        requestTimeoutMillis = requestTimeout.inWholeMilliseconds
    }

    json?.let {
        install(ContentNegotiation) {
            json(json = it)
        }
    }

    if (useCookies) {
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    /*TODO: engine {
        config {
            sslSocketFactory()
        }
    }*/

    block()
}

private val defaultHttpClient = cpsHttpClient { }
internal val defaultJson = Json { ignoreUnknownKeys = true }


internal suspend inline fun<reified T> HttpClient.getAs(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = this.get(urlString = urlString, block = block).body()

internal suspend inline fun HttpClient.getText(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): String = this.get(urlString = urlString, block = block).bodyAsText()


