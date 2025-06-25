package com.demich.cps.platforms.api.dmoj

object DmojUrls {
    const val main = "https://dmoj.ca"
    fun user(username: String) = "$main/user/$username"
}