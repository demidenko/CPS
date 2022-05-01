package com.demich.cps.contests.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.contests.Contest
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.ClistResource

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): CPSDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    val enabledPlatforms = itemEnumSet(name = "enabled_platforms", defaultValue = Contest.platforms.toSet())
    val lastReloadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms", defaultValue = emptySet())
    val ignoredContests = itemJsonable<List<Pair<Contest.Platform, String>>>(name = "ignored_contests_list", defaultValue = emptyList())

    val clistApiAccess = itemJsonable(name = "clist_api_access", defaultValue = CListApi.ApiAccess("", ""))
    val clistAdditionalResources = itemJsonable<List<ClistResource>>(name = "clist_additional_resources", defaultValue = emptyList())
    val clistLastReloadedAdditionalResources = itemJsonable<Set<Int>>(name = "clist_additional_last_reloaded", defaultValue = emptySet())
}