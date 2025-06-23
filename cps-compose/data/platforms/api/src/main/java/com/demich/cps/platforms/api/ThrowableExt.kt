package com.demich.cps.platforms.api

import com.demich.cps.platforms.api.codeforces.CodeforcesApiException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

val Throwable.niceMessage: String? get() =
    when (this) {
        is java.net.UnknownHostException,
        is java.net.SocketException,
        is SocketTimeoutException -> "Connection failed"

        is ResponseException -> HttpStatusCode.fromValue(response.status.value).toString()

        is CodeforcesApiException -> message

        is HttpRequestTimeoutException -> "Request timeout"

        is kotlinx.serialization.SerializationException -> "Serialization failed"

        else -> null
    }

val Throwable.isResponseException get() =
    this is ResponseException

val Throwable.isPageNotFound get() =
    this is ClientRequestException && response.status == HttpStatusCode.NotFound

val Throwable.isRedirect get() =
    this is RedirectResponseException