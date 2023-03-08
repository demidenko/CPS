package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.utils.AtCoderUtils
import com.demich.cps.data.api.AtCoderApi

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaders.atcoder) {
    override suspend fun loadContests(platform: Contest.Platform): List<Contest> =
        AtCoderUtils.extractContests(source = AtCoderApi.getContestsPage())
}