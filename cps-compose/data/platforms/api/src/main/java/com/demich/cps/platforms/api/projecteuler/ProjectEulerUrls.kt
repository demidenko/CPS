package com.demich.cps.platforms.api.projecteuler

object ProjectEulerUrls {
    const val main = "https://projecteuler.net"
    val news get() = "$main/news"

    fun problem(id: Int) = "$main/problem=$id"
}