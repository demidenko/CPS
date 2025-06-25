package com.demich.cps.platforms.api.acmp

object ACMPUrls {
    const val main = "https://acmp.ru"
    fun user(id: Int) = "$main/index.asp?main=user&id=$id"
}