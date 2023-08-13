package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.platforms.api.AtCoderApi

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaderType.atcoder_parse) {
    override suspend fun loadContests(platform: Contest.Platform): List<Contest> =
        AtCoderUtils.extractContests(source = AtCoderApi.getContestsPage())
}