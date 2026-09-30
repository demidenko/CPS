package com.demich.cps.platforms.utils.codeforces


enum class CodeforcesColorTag {
    BLACK,
    GRAY,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    ORANGE,
    RED,
    LEGENDARY,
    ADMIN;

    companion object {
        fun fromRating(rating: Int): CodeforcesColorTag =
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

        fun fromRating(rating: Int?): CodeforcesColorTag =
            if (rating == null) BLACK else fromRating(rating)
    }
}

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

        fun fromRating(rating: Int?): CodeforcesRatingColorTag? =
            if (rating == null) null else fromRating(rating)
    }
}

fun CodeforcesRatingColorTag.toCodeforcesColorTag(): CodeforcesColorTag =
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

fun CodeforcesUserTag.toCodeforcesColorTag(): CodeforcesColorTag =
    when (this) {
        is CodeforcesRatingColorTag -> toCodeforcesColorTag()
        is CodeforcesAdminTag -> ADMIN
        is CodeforcesUnratedTag -> BLACK
    }

fun CodeforcesColorTag.toCodeforcesUserTag(): CodeforcesUserTag =
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