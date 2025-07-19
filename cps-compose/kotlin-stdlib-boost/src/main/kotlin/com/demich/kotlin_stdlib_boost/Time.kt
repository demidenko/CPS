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
            // floor(this / other) <= floor((a1 * k + b1) / (a2 * k)) <= floor(a1 / a2)
            // floor(a1 / (a2 + 1)) <= floor(this / other) <= floor(a1 / a2)

            // let p = k / b2
            // floor(a1 / (a2 * p + 1)) * p + floor((a1 % (a2 * p + 1) - 1) ?: 0 / a2) <= floor(this / other)

            var a = a1
            var b = b1
            while (a > a2 || a == a2 && b >= b2) {
                if (a == a2) return (b - b2).nanoseconds

                // now a > a2
                val div: Long
                val p = k / b2
                if (p > (a - 1) / a2) {
                    div = (a - 1) / a2
                } else {
                    val w: Long = a2 * p + 1
                    val l = a / w
                    val r = a % w
                    div = l * p + (if (r > 0) (r - 1) / a2 else 0)
                }

                val l = div / k
                val r = div % k
                val a3 = a2 * div + b2 * l + (b2 * r) / k
                val b3 = (b2 * r % k).toInt()
                a -= a3
                b -= b3
                if (b < 0) {
                    b += k
                    a -= 1
                }
            }

            return a.seconds + b.nanoseconds
        }
    }
}

private const val k = 1_000_000_000