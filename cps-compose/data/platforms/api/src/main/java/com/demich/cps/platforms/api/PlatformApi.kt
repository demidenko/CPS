package com.demich.cps.platforms.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface PlatformApi {
    val client: HttpClient get() = defaultHttpClient
}

internal fun cpsHttpClient(
    json: Json? = null,
    connectionTimeout: Duration = 15.seconds,
    requestTimeout: Duration = 30.seconds,
    block: HttpClientConfig<*>.() -> Unit
) = HttpClient {
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


val Throwable.niceMessage: String? get() =
    when (this) {
        is java.net.UnknownHostException,
        is java.net.SocketException,
        is SocketTimeoutException -> "Connection failed"

        is ResponseException -> HttpStatusCode.fromValue(response.status.value).toString()

        is CodeforcesApi.CodeforcesTemporarilyUnavailableException -> message

        is kotlinx.serialization.SerializationException -> "Parse failed"

        else -> null
    }

val Throwable.isPageNotFound get() =
    this is ClientRequestException && response.status == HttpStatusCode.NotFound

val Throwable.isRedirect get() =
    this is RedirectResponseException