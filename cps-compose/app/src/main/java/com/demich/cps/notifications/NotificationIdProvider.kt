package com.demich.cps.notifications

private class Allocator {
    private var firstUnused = 0
    operator fun invoke(size: Int) =
        firstUnused.also {
            require(size > 0)
            //it + size <= Int.MAX_VALUE
            require(it <= Int.MAX_VALUE - size)
            firstUnused += size
        }
}

abstract class NotificationIdProvider(val rangeLength: Int) {
    private val allocator = Allocator()

    protected fun nextId(): Int = allocator(size = 1)

    protected fun nextIdRange(): IntRange =
        allocator(size = rangeLength).let { start ->
            start until start + rangeLength
        }
}