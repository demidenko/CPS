package com.demich.cps.contests

import android.content.Context
import com.demich.cps.contests.settings.ContestsSettingsSnapshot
import com.demich.cps.utils.emptyTimedCollection
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper

class ContestsInfoDataStore(context: Context): ItemizedDataStore(context.contests_info_datastore) {
    companion object {
        private val Context.contests_info_datastore by dataStoreWrapper(name = "contests_list_info")
    }

    val ignoredContests = jsonCPS.item(name = "ignored_contests") {
        emptyTimedCollection<ContestCompositeId>()
    }

    val settingsSnapshot = jsonCPS.item<ContestsSettingsSnapshot?>(
        name = "settings_snapshot",
        defaultValue = null
    )
}