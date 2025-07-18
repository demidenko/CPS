package com.demich.kotlin_stdlib_boost

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

operator fun Duration.rem(other: Duration): Duration {
    if (other == Duration.ZERO) {
        throw ArithmeticException("division by zero")
    }

    if (this == Duration.ZERO) {
        return Duration.ZERO
    }

    if (this.isInfinite()) {
        throw ArithmeticException("ambiguous result")
    }

    if (other.isInfinite()) {
        return this
    }

    /*
    int arithmetics:
    10 % 3          = 1
    10 % (-3)       = 1
    (-10) % 3       = -1
    (-10) % (-3)    = -1
     */

    if (other.isNegative()) {
        return rem(-other)
    }

    if (this.isNegative()) {
        return -rem(other)
    }

    check(this.isPositive())
    check(other.isPositive())

    // this = a1 * k + b1
    // other = a2 * k + b2
    // 0 <= a1, a2
    // k = 1e9 (1 second)
    // 0 <= b1, b2 < k

    toComponents { a1, b1 ->
        other.toComponents { a2, b2 ->
            if (a2 == 0L) {
                // return (a1 * k + b1) % b2
                val value: Long = ((a1 % b2) * k + b1) % b2
                return value.nanoseconds
            }

            if (b2 == 0) {
                return (a1 % a2).seconds + b1.nanoseconds
            }

            // this % other = this - floor(this / other) * other

            // this / other > (a1 * k) / (a2 * k + k)
            // this / other < (a1 * k + k) / (a2 * k)
            // a1 / (a2 + 1) < this / other < (a1 + 1) / a2
            // floor(a1 / (a2 + 1)) <= floor(this / other) <= floor((a1 + 1) / a2)

            TODO()
        }
    }
}

private const val k = 1_000_000_000