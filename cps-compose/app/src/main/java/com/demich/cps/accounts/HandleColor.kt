package com.demich.cps.accounts

enum class HandleColor {
    GRAY,
    BROWN,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    YELLOW,
    ORANGE,
    RED;

    companion object {
        //TODO atcoder::orange == cf::red
        val rankedCodeforces    = arrayOf(GRAY, GRAY, GREEN, CYAN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
        val rankedAtCoder       = arrayOf(GRAY, BROWN, GREEN, CYAN, BLUE, YELLOW, YELLOW, ORANGE, ORANGE, RED)
        //val rankedTopCoder      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, YELLOW, YELLOW, YELLOW, YELLOW, RED)
        //TODO:
        val rankedCodeChef      = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, VIOLET, YELLOW, YELLOW, ORANGE, RED)
        val rankedDmoj          = arrayOf(GRAY, GRAY, GREEN, GREEN, BLUE, VIOLET, VIOLET, ORANGE, ORANGE, RED)
    }
}

data class HandleColorBound(
    val handleColor: HandleColor,
    val ratingUpperBound: Int
)

infix fun HandleColor.to(rating: Int): HandleColorBound =
    HandleColorBound(handleColor = this, ratingUpperBound = rating)