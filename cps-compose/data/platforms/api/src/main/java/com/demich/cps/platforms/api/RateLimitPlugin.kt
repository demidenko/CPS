package com.demich.cps.platforms.api

import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


internal val RateLimitPlugin = createClientPlugin(name = "RateLimitPlugin", ::RateLimitPluginConfig) {
    val limits = pluginConfig.limits.also {
        if (it.isEmpty()) it.add(RateLimitPluginConfig.RateLimit(count = 3, window = 2.seconds))
        it.removeUseless()
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
            while (true) {
                // remove unnecessary
                while (recentRuns.isNotEmpty() && recentRuns.first() + maxWindow < currentTime()) {
                    recentRuns.removeFirst()
                }

                val t = recentRuns.firstOrNull() ?: break
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


private fun MutableList<RateLimitPluginConfig.RateLimit>.removeUseless() {
    // we need only nested items i.e. [a.count < b.count and a.window < b.window]
    // additionally check fractions i.e. [10 per minute] is useless if there are [1 per 7 seconds]
    sortBy { it.count }
    var sz = 0
    forEach {
        while (sz > 0 && get(sz - 1).run { count == it.count && window < it.window }) sz -= 1
        repeat(sz) { index ->
            val item = get(index)
            if (it.count % item.count == 0) {
                val k = it.count / item.count
                if (item.window * k > it.window) return@forEach
            }
        }
        if (sz == 0 || get(sz - 1).run { count < it.count && window < it.window}) {
            set(sz, it)
            sz += 1
        }
    }
    while (size > sz) removeAt(lastIndex)
}