package com.demich.cps.notifications

private const val intervalLength = 1 shl 20 // = 2**20

abstract class NotificationIdProvider {
    private var firstUnused = 0

    private fun alloc(size: Int) =
        firstUnused.also {
            require(it <= Int.MAX_VALUE - size)
            firstUnused += size
        }

    protected fun nextId() = alloc(1)
    protected fun nextIdInterval() = IntervalId(alloc(intervalLength))
    protected fun nextRangeId() = alloc(intervalLength).let { start ->
        IntRange(start, start + intervalLength)
    }

    @JvmInline
    value class IntervalId(private val start: Int) {
        operator fun invoke(int: Int) = start + int.mod(intervalLength)
        operator fun invoke(long: Long) = start + long.mod(intervalLength)
        operator fun invoke(str: String) = invoke(str.hashCode())
    }
}