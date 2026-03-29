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