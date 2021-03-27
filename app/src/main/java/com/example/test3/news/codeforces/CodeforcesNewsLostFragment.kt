package com.example.test3.news.codeforces

import com.example.test3.CodeforcesTitle

class CodeforcesNewsLostFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.LOST
    override val isManagesNewEntries = true
    override val isAutoUpdatable = true
    override val viewAdapter = CodeforcesNewsItemsLostRecentAdapter()

    override suspend fun parseData(lang: String) = true
}