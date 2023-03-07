package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.utils.AtCoderApi
import com.demich.cps.utils.AtCoderUtils

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaders.atcoder) {
    override suspend fun loadContests(platform: Contest.Platform): List<Contest> =
        AtCoderUtils.extractContests(source = AtCoderApi.getContestsPage())
}