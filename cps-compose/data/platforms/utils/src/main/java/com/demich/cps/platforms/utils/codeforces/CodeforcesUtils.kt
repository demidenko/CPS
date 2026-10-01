package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.parseHtmlElement


suspend fun CodeforcesPageContentProvider.getUserTagOrNull(handle: String): CodeforcesUserTag? =
    with(CodeforcesRatedUserSelectorImpl()) {
        getUserPage(handle).parseDocument()
            .selectFirst("div.userbox")
            ?.selectRatedUser()
            ?.extractRatedUser()
            ?.userTag
    }

suspend fun CodeforcesPageContentProvider.getHandleSuggestions(str: String): Sequence<CodeforcesHandle> =
    with(CodeforcesRatedUserSelectorImpl()) {
        getHandleSuggestionsPage(str)
            .splitToSequence('\n')
            .filter { it.isNotEmpty() }
            .mapNotNull {
                val i = it.lastIndexOf('|')
                it.substring(i + 1).parseHtmlElement()
                    .selectRatedUser()
                    ?.extractRatedUser()
            }
    }

