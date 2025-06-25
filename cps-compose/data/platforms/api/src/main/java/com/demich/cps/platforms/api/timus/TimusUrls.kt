package com.demich.cps.platforms.api.timus

object TimusUrls {
    const val main = "https://timus.online"
    fun user(id: Int) = "$main/author.aspx?id=$id"
}