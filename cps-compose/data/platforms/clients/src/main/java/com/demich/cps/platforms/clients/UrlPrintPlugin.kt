package com.demich.cps.platforms.clients

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin

internal val UrlPrintPlugin get() = createClientPlugin(name = "UrlPrintingPlugin") {
    on(Send) { request ->
        with(request) {
            println("sending request: ${url.buildString()}")
            val parameters = url.parameters.build()
            if (!parameters.isEmpty()) {
                println("${url.pathSegments.joinToString(separator = "/")} parameters:")
                parameters.forEach { key, values ->
                    println("\t$key: ${if (values.size == 1) values[0] else values}")
                }
            }
        }
        proceed(request)
    }
}