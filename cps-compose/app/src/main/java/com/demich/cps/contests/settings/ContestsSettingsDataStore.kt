package com.demich.cps.contests.settings

import android.content.Context
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.clist.ClistApi
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.combine
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
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

    private val enabledKnownPlatforms = itemEnumSet<Contest.Platform>(name = "enabled_platforms")
    suspend fun changeEnabled(platform: Contest.Platform, enabled: Boolean) {
        require(platform != Contest.Platform.unknown)
        enabledKnownPlatforms.edit {
            if (enabled) add(platform) else remove(platform)
        }
    }

    val clistApiAccess = jsonCPS.item(name = "clist_api_access", defaultValue = ClistApi.ApiAccess("", ""))
    val clistAdditionalResources = jsonCPS.itemList<ClistResource>(name = "clist_additional_resources")

    val enabledPlatforms = combine(
        enabledKnownPlatforms,
        clistAdditionalResources
    ) { platforms, clistAdditional ->
        buildSet {
            addAll(platforms)
            if (clistAdditional.isNotEmpty()) add(Contest.Platform.unknown)
        }
    }

    val contestsDateConstraints = jsonCPS.item(name = "contests_date_constraints") {
        ContestDateRelativeConstraints(
            maxDuration = 30.days,
            nowToStartTimeMaxDuration = 120.days,
            endTimeToNowMaxDuration = 7.days
        )
    }

    val contestsLoadersPriorityLists = jsonCPS.itemMap(name = "loading_priorities") {
        ContestsLoaderType.entries.sortedWith(
            compareByDescending<ContestsLoaderType> { it.supportedPlatforms.size }.thenBy { it.ordinal }
        ).let { loaders ->
            Contest.platforms.associateWith { platform ->
                loaders.filter { platform in it.supportedPlatforms }
            }
        }
    }

    val autoUpdateInterval = jsonCPS.itemNullable<Duration>(name = "autoupdate_interval")
}

@Serializable
data class ContestDateRelativeConstraints(
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