package com.demich.cps.platforms.utils.atcoder

import com.demich.cps.platforms.api.atcoder.AtCoderRatingChange
import com.demich.cps.platforms.api.atcoder.AtCoderUrls

fun AtCoderRatingChange.contestId(): String {
    val s = StandingsUrl.removePrefix("/contests/")
    return s.substring(0, s.indexOf('/'))
}

fun AtCoderRatingChange.url(handle: String): String =
    AtCoderUrls.userContestResult(handle, contestId())