package com.demich.cps.contests.settings

import android.content.Context
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateBaseConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.ClistClient
import com.demich.cps.platforms.api.ClistResource
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.flowOf
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): ItemizedDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by dataStoreWrapper("contests_settings")
    }

    private val enabledPlatforms = itemEnumSet<Contest.Platform>(name = "enabled_platforms")
    fun flowOfEnabledPlatforms(): Flow<Set<Contest.Platform>> = flowOf {
        buildSet {
            addAll(it[enabledPlatforms])
            if (it[clistAdditionalResources].isNotEmpty()) add(Contest.Platform.unknown)
        }
    }
    suspend fun changeEnabled(platform: Contest.Platform, enabled: Boolean) {
        require(platform != Contest.Platform.unknown)
        enabledPlatforms.edit {
            if (enabled) add(platform) else remove(platform)
        }
    }

    val clistApiAccess = jsonCPS.item(name = "clist_api_access", defaultValue = ClistClient.ApiAccess("", ""))
    val clistAdditionalResources = jsonCPS.itemList<ClistResource>(name = "clist_additional_resources")

    val contestsDateConstraints = jsonCPS.item(name = "contests_date_constraints") {
        ContestDateBaseConstraints(
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

