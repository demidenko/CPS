package com.example.test3.news.codeforces

import com.example.test3.CodeforcesTitle
import com.example.test3.utils.CodeforcesAPI

class CodeforcesNewsRecentFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.RECENT
    override val isManagesNewEntries = false
    override val isAutoUpdatable = false
    override val viewAdapter = CodeforcesRecentActionsAdapter()

    override suspend fun parseData(lang: String): Boolean {
        val source = CodeforcesAPI.getPageSource("/recent-actions", lang) ?: return false
        return viewAdapter.parseData(source)
    }

}