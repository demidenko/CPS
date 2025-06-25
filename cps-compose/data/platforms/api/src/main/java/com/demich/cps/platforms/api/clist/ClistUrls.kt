package com.demich.cps.platforms.api.clist

object ClistUrls {
    const val main = "https://clist.by"
    fun user(login: String) = "$main/coder/$login"

    const val api = "$main/api/v4"
    val apiHelp get() = "$api/doc/"
}