package com.demich.cps.utils

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
import java.net.SocketException
import java.net.UnknownHostException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class ResourceApi {
    protected open val client: HttpClient = defaultHttpClient
    protected val json = Json { ignoreUnknownKeys = true }

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

internal val defaultHttpClient = cpsHttpClient { }


suspend inline fun<reified T> HttpClient.getAs(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = this.get(urlString = urlString, block = block).body()

suspend inline fun HttpClient.getText(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): String = this.get(urlString = urlString, block = block).bodyAsText()


val Throwable.niceMessage: String? get() =
    when (this) {
        is UnknownHostException, is SocketException, is SocketTimeoutException -> "Connection failed"
        is ClientRequestException -> HttpStatusCode.fromValue(response.status.value).toString()
        is kotlinx.serialization.SerializationException -> "Parse error"
        else -> null
    }

val Throwable.isPageNotFound get() =
    this is ClientRequestException && response.status == HttpStatusCode.NotFound