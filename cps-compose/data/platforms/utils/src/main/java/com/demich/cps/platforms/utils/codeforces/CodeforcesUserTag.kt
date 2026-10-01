package com.demich.cps.platforms.utils.codeforces


sealed interface CodeforcesUserTag

data object CodeforcesUnratedTag: CodeforcesUserTag
data object CodeforcesAdminTag: CodeforcesUserTag

enum class CodeforcesRatingColorTag: CodeforcesUserTag {
    GRAY,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    ORANGE,
    RED,
    LEGENDARY
    ;

    companion object {
        fun fromRating(rating: Int): CodeforcesRatingColorTag =
            when {
                rating < 1200 -> GRAY
                rating < 1400 -> GREEN
                rating < 1600 -> CYAN
                rating < 1900 -> BLUE
                rating < 2100 -> VIOLET
                rating < 2400 -> ORANGE
                rating < 3000 -> RED
                else -> LEGENDARY
            }
    }
}

enum class CodeforcesHandleCssTag {
    // do not rename entries!
    BLACK,
    GRAY,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    ORANGE,
    RED,
    LEGENDARY,
    ADMIN
}

fun CodeforcesRatingColorTag.toCssTag(): CodeforcesHandleCssTag =
    when (this) {
        GRAY -> GRAY
        GREEN -> GREEN
        CYAN -> CYAN
        BLUE -> BLUE
        VIOLET -> VIOLET
        ORANGE -> ORANGE
        RED -> RED
        LEGENDARY -> LEGENDARY
    }

fun CodeforcesUserTag.toCssTag(): CodeforcesHandleCssTag =
    when (this) {
        is CodeforcesRatingColorTag -> toCssTag()
        is CodeforcesAdminTag -> ADMIN
        is CodeforcesUnratedTag -> BLACK
    }

fun CodeforcesHandleCssTag.toUserTag(): CodeforcesUserTag =
    when (this) {
        BLACK -> CodeforcesUnratedTag
        GRAY -> CodeforcesRatingColorTag.GRAY
        GREEN -> CodeforcesRatingColorTag.GREEN
        CYAN -> CodeforcesRatingColorTag.CYAN
        BLUE -> CodeforcesRatingColorTag.BLUE
        VIOLET -> CodeforcesRatingColorTag.VIOLET
        ORANGE -> CodeforcesRatingColorTag.ORANGE
        RED -> CodeforcesRatingColorTag.RED
        LEGENDARY -> CodeforcesRatingColorTag.LEGENDARY
        ADMIN -> CodeforcesAdminTag
    }