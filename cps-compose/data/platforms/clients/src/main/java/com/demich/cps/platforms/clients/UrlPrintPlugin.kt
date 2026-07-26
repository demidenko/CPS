package com.demich.cps.platforms.clients

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder

internal val UrlPrintPlugin get() = createClientPlugin(name = "UrlPrintingPlugin") {
    on(Send) { request ->
        println(request.urlMessage())
        proceed(request)
    }
}

private fun HttpRequestBuilder.urlMessage(): String = buildString {
    append("sending request: ${url.buildString()}")
    val parameters = url.parameters.build()
    if (!parameters.isEmpty()) {
        appendLine()
        append("${url.pathSegments.joinToString(separator = "/")} parameters:")
        parameters.forEach { key, values ->
            appendLine()
            append("\t$key: ${if (values.size == 1) values[0] else values}")
        }
    }
}