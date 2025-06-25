package com.demich.cps.platforms.api.codechef

object CodeChefUrls {
    const val main = "https://www.codechef.com"
    fun user(username: String) = "$main/users/$username"
}