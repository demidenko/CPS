package com.demich.cps.contests

import android.content.Context
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant

class ContestsInfoDataStore(context: Context): ItemizedDataStore(context.contests_info_datastore) {
    companion object {
        private val Context.contests_info_datastore by dataStoreWrapper(name = "contests_list_info")
    }

    val lastReloadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms")
    val clistLastReloadedAdditionalResources = jsonCPS.itemSet<Int>(name = "clist_additional_last_reloaded")

    val ignoredContests = jsonCPS.itemMap<Pair<Contest.Platform, String>, Instant>(name = "ignored_contests")
}