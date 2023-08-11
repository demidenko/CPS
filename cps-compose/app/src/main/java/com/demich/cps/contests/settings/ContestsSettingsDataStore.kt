package com.demich.cps.contests.settings

import android.content.Context
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.utils.jsonCPS
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.ClistResource
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): ItemizedDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by dataStoreWrapper("contests_settings")
    }

    val enabledPlatforms = itemEnumSet<Contest.Platform>(
        name = "enabled_platforms",
        defaultValue = emptySet()
    ).mapGetter { platforms ->
        //This set must contain Platform.unknown
        Contest.Platform.unknown.let { if (it in platforms) platforms else platforms + it }
    }
    val lastReloadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms", defaultValue = emptySet())
    val ignoredContests = jsonCPS.item<Map<Pair<Contest.Platform, String>, Instant>>(name = "ignored_contests", defaultValue = emptyMap())

    val clistApiAccess = jsonCPS.item(name = "clist_api_access", defaultValue = ClistApi.ApiAccess("", ""))
    val clistAdditionalResources = jsonCPS.item<List<ClistResource>>(name = "clist_additional_resources", defaultValue = emptyList())
    val clistLastReloadedAdditionalResources = jsonCPS.item<Set<Int>>(name = "clist_additional_last_reloaded", defaultValue = emptySet())

    val contestsDateConstraints = jsonCPS.item(
        name = "contests_date_constraints",
        defaultValue = {
            ContestDateBaseConstraints(
                maxDuration = 30.days,
                nowToStartTimeMaxDuration = 120.days,
                endTimeToNowMaxDuration = 7.days
            )
        }
    )

    val contestsLoadersPriorityLists = jsonCPS.item(name = "loading_priorities", defaultValue = ::makeDefaultLoadingPriorities)

    val enabledAutoUpdate = itemBoolean(name = "auto_update", defaultValue = true)
}

@Serializable
data class ContestDateBaseConstraints(
    val maxDuration: Duration,
    val nowToStartTimeMaxDuration: Duration,
    val endTimeToNowMaxDuration: Duration,
) {
    fun at(currentTime: Instant) = ContestDateConstraints(
        maxStartTime = currentTime + nowToStartTimeMaxDuration,
        minEndTime = currentTime - endTimeToNowMaxDuration,
        maxDuration = maxDuration
    )
}

data class ContestDateConstraints(
    val maxStartTime: Instant = Instant.DISTANT_FUTURE,
    val minEndTime: Instant = Instant.DISTANT_PAST,
    val maxDuration: Duration = Duration.INFINITE
) {
    fun check(startTime: Instant, duration: Duration): Boolean {
        return duration <= maxDuration && startTime <= maxStartTime && startTime + duration >= minEndTime
    }
}

private fun makeDefaultLoadingPriorities() =
    ContestsLoaders.entries.sortedWith(
        compareByDescending<ContestsLoaders> { it.supportedPlatforms.size }.thenBy { it.ordinal }
    ).let { loaders ->
        Contest.platforms.associateWith { platform ->
            buildList {
                loaders.forEach { if (platform in it.supportedPlatforms) add(it) }
            }
        }
    }
