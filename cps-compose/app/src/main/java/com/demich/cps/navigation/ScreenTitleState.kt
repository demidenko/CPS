package com.demich.cps.navigation

import androidx.compose.runtime.Stable

@Stable
fun interface ScreenTitleState {
    fun title(): String
}

private fun Iterable<String>.joinToScreenTitle(): String =
    joinToString(prefix = "::", separator = ".", transform = String::lowercase)

fun cpsScreenTitleOf(vararg strings: String) = strings.asIterable().joinToScreenTitle()

data class ScreenStaticTitleState(private val tokens: List<String>): ScreenTitleState {
    constructor(vararg strings: String): this(strings.asList())

    override fun title() = tokens.joinToScreenTitle()
}
