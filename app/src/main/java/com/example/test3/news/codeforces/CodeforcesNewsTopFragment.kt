package com.example.test3.news.codeforces

import com.example.test3.CodeforcesTitle
import com.example.test3.utils.CodeforcesAPI

class CodeforcesNewsTopFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.TOP
    override val isManagesNewEntries = false
    override val isAutoUpdatable = false
    override val viewAdapter = CodeforcesBlogEntriesAdapter()

    override suspend fun parseData(lang: String): Boolean {
        val source = CodeforcesAPI.getPageSource("/top", lang) ?: return false
        return viewAdapter.parseData(source)
    }

}