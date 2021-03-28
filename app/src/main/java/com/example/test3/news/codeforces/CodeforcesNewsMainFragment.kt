package com.example.test3.news.codeforces

import com.example.test3.CodeforcesTitle
import com.example.test3.utils.CodeforcesAPI

class CodeforcesNewsMainFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.MAIN
    override val isManagesNewEntries = true
    override val isAutoUpdatable = false
    override val viewAdapter = CodeforcesBlogEntriesAdapter()

    override suspend fun parseData(lang: String): Boolean {
        val source = CodeforcesAPI.getPageSource("/", lang) ?: return false
        return viewAdapter.parseData(source)
    }

}