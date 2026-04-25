package com.demich.cps.platforms.clients

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
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
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val defaultHttpClient = cpsHttpClient { }

internal fun cpsHttpClient(
    json: Json? = null,
    useCookies: Boolean = false,
    connectionTimeout: Duration = 15.seconds,
    requestTimeout: Duration = 30.seconds,
    retryOnExceptionIf: (Throwable) -> Boolean = { false },
    block: HttpClientConfig<*>.() -> Unit
) = HttpClient(engineFactory = SingleOkHttpEngineFactory) {
    expectSuccess = true

    //careful!!! only one install and retry block is used in ktor
    install(HttpRequestRetry) {
        retryOnExceptionIf(maxRetries = 3) { requestBuilder, throwable ->
            throwable.shouldRetry() || retryOnExceptionIf(throwable)
        }
        delayMillis { 500/*.milliseconds*/ }
    }

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

private object SingleOkHttpEngineFactory: HttpClientEngineFactory<OkHttpConfig> {

    private val okHttpEngine by lazy { OkHttp.create() }

    override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine =
        okHttpEngine
}

private fun Throwable.shouldRetry(): Boolean =
    when (this) {
        is kotlinx.coroutines.CancellationException -> false
        is java.net.UnknownHostException -> false
        is kotlinx.io.IOException -> true
        is ResponseException if response.status.isServerError() -> true
        else -> false
    }

internal val defaultJson = Json { ignoreUnknownKeys = true }


internal suspend inline fun<reified T> HttpClient.getAs(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = this.get(urlString = urlString, block = block).body()

internal suspend inline fun HttpClient.getText(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): String = this.get(urlString = urlString, block = block).bodyAsText()


