@file:OptIn(ExperimentalTime::class)

package com.demich.cps.platforms.clients

import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


internal val RateLimitPlugin = createClientPlugin(name = "RateLimitPlugin", ::RateLimitPluginConfig) {
    val limits = pluginConfig.limits.let {
        if (it.isEmpty()) it.add(RateLimitPluginConfig.RateLimit(count = 3, window = 2.seconds))
        removeUseless(it)
    }

    val maxWindow = limits.maxOf { it.window }

    val mutex = Mutex()
    val recentRuns = ArrayDeque<Instant>()

    fun currentTime(): Instant = Clock.System.now()

    fun executionAllowed(rateLimit: RateLimitPluginConfig.RateLimit): Boolean {
        val (count, window) = rateLimit
        val t = currentTime()
        val runsInCurrentWindow = recentRuns.count { it >= t - window }
        return runsInCurrentWindow < count
    }

    //TODO: do not delay on connection errors (no onResponse call)
    onRequest { request, _ ->
        mutex.withLock {
            while (recentRuns.isNotEmpty()) {
                val t = recentRuns.first()

                // remove unnecessary
                if (t + maxWindow < currentTime()) {
                    recentRuns.removeFirst()
                    continue
                }

                val limit = limits.firstOrNull { !executionAllowed(it) } ?: break
                delay(t + limit.window - currentTime())
            }

            recentRuns.addLast(currentTime())
        }
    }

    onResponse {

    }
}

internal class RateLimitPluginConfig {
    internal val limits = mutableListOf<RateLimit>()

    internal data class RateLimit(val count: Int, val window: Duration) {
        init {
            require(count > 0) { "count must be positive"}
            require(window.isPositive()) { "window must be positive"}
        }
    }

    infix fun Int.per(window: Duration) {
        limits.add(RateLimit(count = this, window = window))
    }
}

private fun RateLimitPluginConfig.RateLimit.isUselessIf(other: RateLimitPluginConfig.RateLimit): Boolean {
    if (window <= other.window && count >= other.count) {
        return true
    }
    if (count % other.count == 0) {
        // [10 per minute] is useless if there are [1 per 7 seconds]
        val k = count / other.count
        return window <= other.window * k
    }
    // TODO: if (window % other.window == 0)
    return false
}


private fun removeUseless(limits: List<RateLimitPluginConfig.RateLimit>) =
    buildList {
        limits.forEachIndexed { index, limit ->
            if (none { limit.isUselessIf(it) }) {
                if((index+1 until limits.size).none { limit.isUselessIf(limits[it]) }) {
                    add(limit)
                }
            }
        }
    }