package com.demich.cps.platforms.clients

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface PlatformClient {
    val client: HttpClient get() = defaultHttpClient
}

internal fun cpsHttpClient(
    json: Json? = null,
    useCookies: Boolean = false,
    connectionTimeout: Duration = 15.seconds,
    requestTimeout: Duration = 30.seconds,
    retryOnExceptionIf: (Throwable) -> Boolean = { false },
    block: HttpClientConfig<*>.() -> Unit
) = HttpClient(OkHttp) {
    expectSuccess = true

    install(HttpTimeout) {
        connectTimeoutMillis = connectionTimeout.inWholeMilliseconds
        requestTimeoutMillis = requestTimeout.inWholeMilliseconds
    }

    //careful!!! only one install and retry block is used in ktor
    install(HttpRequestRetry) {
        retryOnExceptionIf(maxRetries = 3) { requestBuilder, throwable ->
            throwable.shouldRetry() || retryOnExceptionIf(throwable)
        }
        delayMillis { 500/*.milliseconds*/ }
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

private fun Throwable.shouldRetry(): Boolean {
    if (this is CancellationException) return false
    if (this is IOException) return true
    if (this is ResponseException && response.status.isServerError()) return true
    return false
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


