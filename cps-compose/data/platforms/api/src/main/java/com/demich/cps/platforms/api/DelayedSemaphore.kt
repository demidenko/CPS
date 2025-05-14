package com.demich.cps.platforms.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

internal class DelayedSemaphore(
    permits: Int,
    val limit: Duration
    //TODO: permitsPerSecond???
) {
    private val mutex = Mutex()
    private val semaphore = Semaphore(permits = permits)
    private var lastRun = Instant.DISTANT_PAST

    suspend inline fun <T> withPermit(action: () -> T): T =
        semaphore.withPermit {
            mutex.withLock {
                (currentTime() - lastRun).let {
                    //if it < 0 ????
                    if (it < limit) delay(limit - it)
                }
                lastRun = currentTime()
            }
            action()
        }

    private fun currentTime() = Clock.System.now()
}
