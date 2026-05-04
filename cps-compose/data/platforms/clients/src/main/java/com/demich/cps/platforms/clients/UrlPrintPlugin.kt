package com.demich.cps.platforms.clients

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder

internal fun UrlPrintPlugin(
    name: String,
    block: HttpRequestBuilder.() -> Unit
) = createClientPlugin(name = "${name}UrlPrintingPlugin") {
    on(Send) { request ->
        request.block()
        proceed(request)
    }
}

internal fun HttpRequestBuilder.parametersPrettyPrint() {
    url.parameters.build().forEach { key, values ->
        println("\t$key: ${if (values.size == 1) values[0] else values}")
    }
}