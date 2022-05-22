package com.demich.cps.news.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.codeforces.CodeforcesLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsNews: NewsSettingsDataStore
    get() = NewsSettingsDataStore(this)

class NewsSettingsDataStore(context: Context): CPSDataStore(context.news_settings_dataStore) {
    companion object {
        private val Context.news_settings_dataStore by preferencesDataStore("news_settings")
    }

    val codeforcesDefaultTab = itemEnum(name = "cf_default_tab", defaultValue = CodeforcesTitle.TOP)
    val codeforcesLocale = itemEnum(name = "cf_locale", defaultValue = CodeforcesLocale.EN)

    val codeforcesLostEnabled = itemBoolean(name = "cf_lost_enabled", defaultValue = false)

    fun flowOfCodeforcesTabs(): Flow<List<CodeforcesTitle>> {
        return codeforcesLostEnabled.flow.map { lostEnabled ->
            buildList {
                add(CodeforcesTitle.MAIN)
                add(CodeforcesTitle.TOP)
                add(CodeforcesTitle.RECENT)
                if (lostEnabled) add(CodeforcesTitle.LOST)
            }
        }
    }
}