package com.demich.cps.contests.settings

import android.content.Context
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateBaseConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.ClistResource
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): ItemizedDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by dataStoreWrapper("contests_settings")
    }

    val enabledPlatforms = itemEnumSet<Contest.Platform>(name = "enabled_platforms").mapGetter { platforms ->
        //This set must contain Platform.unknown
        Contest.Platform.unknown.let { if (it in platforms) platforms else platforms + it }
    }
    val lastReloadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms")
    val ignoredContests = jsonCPS.itemMap<Pair<Contest.Platform, String>, Instant>(name = "ignored_contests")

    val clistApiAccess = jsonCPS.item(name = "clist_api_access", defaultValue = ClistApi.ApiAccess("", ""))
    val clistAdditionalResources = jsonCPS.itemList<ClistResource>(name = "clist_additional_resources")
    val clistLastReloadedAdditionalResources = jsonCPS.itemSet<Int>(name = "clist_additional_last_reloaded")

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

    val contestsLoadersPriorityLists = jsonCPS.itemMap(name = "loading_priorities", defaultValue = ::makeDefaultLoadingPriorities)

    val enabledAutoUpdate = itemBoolean(name = "auto_update", defaultValue = true)
}

private fun makeDefaultLoadingPriorities() =
    ContestsLoaderType.entries.sortedWith(
        compareByDescending<ContestsLoaderType> { it.supportedPlatforms.size }.thenBy { it.ordinal }
    ).let { loaders ->
        Contest.platforms.associateWith { platform ->
            loaders.filter { platform in it.supportedPlatforms }
        }
    }
