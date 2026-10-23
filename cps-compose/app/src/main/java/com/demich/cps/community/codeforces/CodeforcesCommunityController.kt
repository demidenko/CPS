package com.demich.cps.community.codeforces

import androidx.compose.runtime.Stable


@Stable
class CodeforcesCommunityController(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    val pagerState: CodeforcesCommunityPagerState
): CodeforcesCommunityBackgroundLoader by backgroundLoader {

}

