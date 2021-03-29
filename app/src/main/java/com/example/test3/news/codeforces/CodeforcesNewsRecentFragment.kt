package com.example.test3.news.codeforces

import androidx.lifecycle.lifecycleScope
import com.example.test3.CodeforcesTitle

class CodeforcesNewsRecentFragment(): CodeforcesNewsFragment() {

    override val title = CodeforcesTitle.RECENT
    override val isManagesNewEntries = false
    override val isAutoUpdatable = false
    override val viewAdapter by lazy {
        CodeforcesRecentActionsAdapter(
            lifecycleScope,
            newsFragment.newsViewModel.getRecentActionsData()
        )
    }


}