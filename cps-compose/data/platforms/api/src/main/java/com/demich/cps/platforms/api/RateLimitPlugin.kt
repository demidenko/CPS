package com.demich.cps.platforms.api

import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration


internal val RateLimitPlugin = createClientPlugin(name = "RateLimitPlugin", ::RateLimitPluginConfig) {
    val limits = pluginConfig.limits.also {
        require(it.isNotEmpty()) { "No rate limits are set" }
    }
    val maxWindow = limits.maxOf { it.window }

    val mutex = Mutex()
    val recentRuns = ArrayDeque<Instant>()

    fun canStartNew(rateLimit: RateLimitPluginConfig.RateLimit): Boolean {
        val (count, window) = rateLimit
        val t = currentTime()
        val runsInCurrentWindow = recentRuns.count { it >= t - window }
        return runsInCurrentWindow + 1 <= count
    }

    //TODO: do not delay on connection errors (no onResponse call)
    onRequest { request, _ ->
        mutex.withLock {
            while (limits.any { !canStartNew(it) }) {
                // remove unnecessary
                while (recentRuns.isNotEmpty() && recentRuns.first() + maxWindow < currentTime()) {
                    recentRuns.removeFirst()
                }

                limits.forEach {
                    if (!canStartNew(it)) {
                        recentRuns.firstOrNull()?.let { t ->
                            delay(t + it.window - currentTime())
                        }
                    }
                }
            }

            recentRuns.addLast(currentTime())
        }
    }

    onResponse {

    }
}

internal class RateLimitPluginConfig {
    val limits = mutableListOf<RateLimit>()

    internal data class RateLimit(val count: Int, val window: Duration)

    infix fun Int.per(window: Duration): RateLimit =
        RateLimit(count = this, window = window)
}

private fun currentTime(): Instant = Clock.System.now()