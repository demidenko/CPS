package com.demich.cps.news.codeforces

enum class CodeforcesTitle {
    MAIN, TOP, RECENT, LOST
}

enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String {
        return when(this){
            EN -> "en"
            RU -> "ru"
        }
    }
}