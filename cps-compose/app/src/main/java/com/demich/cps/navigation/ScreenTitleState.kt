package com.demich.cps.navigation

import androidx.compose.runtime.Stable

@Stable
fun interface ScreenTitleState {
    fun title(): String
}

private fun Iterable<String>.makeScreenTitle(): String =
    joinToString(prefix = "::", separator = ".", transform = String::lowercase)

fun cpsScreenTitle(vararg strings: String) = strings.asIterable().makeScreenTitle()

data class ScreenStaticTitleState(private val tokens: List<String>): ScreenTitleState {
    constructor(vararg strings: String): this(strings.asList())

    override fun title() = tokens.makeScreenTitle()
}
