package com.demich.cps.platforms.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class RateLimitingSemaphore(
    permits: Int,
    val minDelay: Duration,
    val window: Duration,
    val permitsPerWindow: Int
) {
    constructor(
        permits: Int,
        minDelay: Duration,
        permitsPerSecond: Int
    ): this(
        permits = permits,
        minDelay = minDelay,
        window = 1.seconds,
        permitsPerWindow = permitsPerSecond
    )

    init {
        require(minDelay.isPositive())
        require(window.isPositive())
        require(permitsPerWindow > 0)
    }

    private val mutex = Mutex()
    private val semaphore = Semaphore(permits = permits)

    private val recentRuns = ArrayDeque<Instant>()

    suspend inline fun <T> withPermit(action: () -> T): T =
        semaphore.withPermit {
            mutex.withLock {
                currentTime().let { t ->
                    // just in fantastic case
                    while (recentRuns.isNotEmpty() && recentRuns.last() > t) recentRuns.removeLast()
                }
                while (currentTime().let { t -> recentRuns.count { it >= t - window } + 1 > permitsPerWindow }) {
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
            action()
        }

    private fun currentTime() = Clock.System.now()
}
