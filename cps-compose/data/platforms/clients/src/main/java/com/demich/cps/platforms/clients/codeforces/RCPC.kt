package com.demich.cps.platforms.clients.codeforces

import com.demich.cps.platforms.clients.decodeAES
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val redirectWaitTime: Duration
    get() = 300.milliseconds

private val RCPC = object {
    private var rcpcToken: String = ""

    private var last_c = ""
    private fun calculateToken(source: String) {
        val i = source.indexOf("c=toNumbers(")
        val c = source.substring(source.indexOf("(\"",i)+2, source.indexOf("\")",i))
        if (c == last_c) return
        rcpcToken = decodeAES(c)
        last_c = c
        println("$c: $rcpcToken")
    }

    private fun String.isRCPCCase() =
        startsWith("<html><body>Redirecting... Please, wait.")

    suspend inline fun getPage(get: (String) -> String): String {
        val s = get(rcpcToken)
        return if (s.isRCPCCase()) {
            calculateToken(s)
            delay(redirectWaitTime)
            get(rcpcToken)
        } else s
    }
}