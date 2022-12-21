package com.demich.cps.contests.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.contests.Contest
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.ClistResource
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): ItemizedDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    //This set will always contains Platform.unknown
    val enabledPlatforms = itemEnumSet(name = "enabled_platforms", defaultValue = setOf(Contest.Platform.unknown))
    val lastReloadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms", defaultValue = emptySet())
    val ignoredContests = jsonCPS.item<Map<Pair<Contest.Platform, String>, Instant>>(name = "ignored_contests", defaultValue = emptyMap())

    val clistApiAccess = jsonCPS.item(name = "clist_api_access", defaultValue = CListApi.ApiAccess("", ""))
    val clistAdditionalResources = jsonCPS.item<List<ClistResource>>(name = "clist_additional_resources", defaultValue = emptyList())
    val clistLastReloadedAdditionalResources = jsonCPS.item<Set<Int>>(name = "clist_additional_last_reloaded", defaultValue = emptySet())

    val contestsDateConstraints = jsonCPS.item(name = "contests_date_constraints", defaultValue = ContestDateConstraints())

    val contestsLoadersPriorityLists = jsonCPS.item(name = "loading_priorities", defaultValue = defaultLoadingPriorities)

}

@Serializable
data class ContestDateConstraints(
    val maxDuration: Duration = 30.days,
    val nowToStartTimeMaxDuration: Duration = 120.days,
    val endTimeToNowMaxDuration: Duration = 7.days,
) {
    fun makeFor(currentTime: Instant) = Current(
        maxStartTime = currentTime + nowToStartTimeMaxDuration,
        minEndTime = currentTime - endTimeToNowMaxDuration,
        maxDuration = maxDuration
    )
    data class Current(
        val maxStartTime: Instant = Instant.DISTANT_FUTURE,
        val minEndTime: Instant = Instant.DISTANT_PAST,
        val maxDuration: Duration = Duration.INFINITE
    ) {
        fun check(startTime: Instant, duration: Duration): Boolean {
            return duration <= maxDuration && startTime <= maxStartTime && startTime + duration >= minEndTime
        }
    }
}

private val defaultLoadingPriorities by lazy {
    Contest.platforms.associateWith { platform ->
        when (platform) {
            Contest.Platform.codeforces -> listOf(
                ContestsLoaders.clist,
                ContestsLoaders.codeforces
            )
            Contest.Platform.atcoder -> listOf(
                ContestsLoaders.clist,
                ContestsLoaders.atcoder
            )
            Contest.Platform.dmoj -> listOf(
                ContestsLoaders.clist,
                ContestsLoaders.dmoj
            )
            else -> listOf(ContestsLoaders.clist)
        }
    }
}
