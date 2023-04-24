package com.demich.cps.notifications

abstract class NotificationIdProvider(val intervalLength: Int) {
    private var firstUnused = 0

    private fun alloc(size: Int) =
        firstUnused.also {
            //it + size - 1 <= Int.MAX_VALUE
            require(it <= Int.MAX_VALUE - size + 1)
            firstUnused += size
        }

    protected fun nextId() = alloc(1)
    protected fun nextIdRange() = alloc(intervalLength).let { start ->
        start until start + intervalLength
    }
}