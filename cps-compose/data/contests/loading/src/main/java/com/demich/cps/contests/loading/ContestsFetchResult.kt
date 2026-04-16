package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform

data class ContestsFetchResult(
    val platform: ContestPlatform,
    val fetchSource: ContestsFetchSource,
    val result: Result<List<Contest>>
) {
    init {
        result.onSuccess {
            it.forEach { contest ->
                check(contest.platform == platform) {
                    "contests fetch result for platform $platform contains ${contest.platform}"
                }
            }
        }
    }
}