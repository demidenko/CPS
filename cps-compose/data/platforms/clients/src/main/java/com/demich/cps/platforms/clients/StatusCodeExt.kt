package com.demich.cps.platforms.clients

import io.ktor.http.HttpStatusCode

// based on ktor's HttpStatusCode.isSuccess()

internal fun HttpStatusCode.isClientError(): Boolean = value in (400 until 500)

internal fun HttpStatusCode.isServerError(): Boolean = value in (500 until 600)