package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.clist.ClistApi
import com.demich.cps.platforms.api.clist.ClistContest
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.platforms.utils.ClistUtils
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlin.time.Duration.Companion.seconds

class ClistContestsLoader(
    val api: ClistApi,
    val apiAccess: ClistApi.ApiAccess,
    val resources: Collection<ClistResource>
): ContestsLoaderMultiple() {
    override val type get() = ContestsLoaderType.clist_api

    override suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ) = api.getContests(
        apiAccess = apiAccess,
        maxStartTime = dateConstraints.maxStartTime,
        minEndTime = dateConstraints.minEndTime,
        resourceIds = ClistUtils.makeResourceIds(platforms, resources)
    ).mapAndFilterResult(dateConstraints)
}


private fun Collection<ClistContest>.mapAndFilterResult(dateConstraints: ContestDateConstraints): List<Contest> {
    val format = DateTimeComponents.Format {
        //YYYY-MM-DDThh:mm:ss
        date(LocalDate.Formats.ISO)
        char('T')
        time(LocalTime.Formats.ISO)
    }

    return mapNotNull { clistContest ->
        val contest = clistContest.toContest { Instant.parse(it, format) }
        if (!dateConstraints.check(contest)) {
            return@mapNotNull null
        }
        when (contest.platform) {
            Contest.Platform.atcoder -> contest.takeIf { clistContest.host == "atcoder.jp" }
            else -> contest
        }
    }
}

private inline fun ClistContest.toContest(parseDate: (String) -> Instant): Contest {
    val platform = Contest.platformsExceptUnknown
        .find { ClistUtils.getClistApiResourceId(it) == resource_id }
        ?: Contest.Platform.unknown

    return Contest(
        platform = platform,
        id = ClistUtils.extractContestId(this, platform),
        title = event,
        startTime = parseDate(start),
        endTime = parseDate(end),
        duration = duration.seconds,
        link = href,
        host = host.takeIf { platform == Contest.Platform.unknown }
    )
}