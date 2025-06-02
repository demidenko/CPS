package com.demich.cps.platforms.api

import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


internal val RateLimitPlugin = createClientPlugin(name = "RateLimitPlugin", ::RateLimitPluginConfig) {
    val minDelay = pluginConfig.minimumDelay
    val window = pluginConfig.window
    val requestsPerWindow = pluginConfig.requestsPerWindow

    val mutex = Mutex()
    val recentRuns = ArrayDeque<Instant>()

    fun runsInCurrentWindow(): Int {
        val t = currentTime()
        return recentRuns.count { it >= t - window }
    }

    //TODO: do not delay on connection errors (no onResponse call)
    onRequest { request, _ ->
        mutex.withLock {
            while (runsInCurrentWindow() + 1 > requestsPerWindow) {
                while (recentRuns.isNotEmpty() && recentRuns.first() + window < currentTime()) {
                    recentRuns.removeFirst()
                }
                recentRuns.firstOrNull()?.let {
                    delay(it + window - currentTime())
                }
            }
            recentRuns.lastOrNull()?.let { lastRun ->
                val d = currentTime() - lastRun
                delay(minDelay - d)
            }
            recentRuns.addLast(currentTime())
        }
    }

    onResponse {

    }
}

internal class RateLimitPluginConfig {
    var minimumDelay: Duration = 50.milliseconds
    var window: Duration = 1.seconds
    var requestsPerWindow: Int = 3
}

private fun currentTime(): Instant = Clock.System.now()