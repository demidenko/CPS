package com.demich.cps.community.settings

import android.content.Context
import com.demich.cps.community.codeforces.CodeforcesTitle
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.utils.isRuSystemLanguage
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsCommunity: CommunitySettingsDataStore
    get() = CommunitySettingsDataStore(this)

class CommunitySettingsDataStore(context: Context): ItemizedDataStore(context.community_settings_dataStore) {
    companion object {
        private val Context.community_settings_dataStore by dataStoreWrapper("community_settings")
    }

    val codeforcesDefaultTab = itemEnum(name = "cf_default_tab", defaultValue = CodeforcesTitle.TOP)
    val codeforcesLocale = jsonCPS.item(name = "cf_locale") {
        if (isRuSystemLanguage()) CodeforcesLocale.RU
        else CodeforcesLocale.EN
    }

    val codeforcesFollowEnabled = itemBoolean(name = "cf_follow_enabled", defaultValue = false)

    val codeforcesLostEnabled = itemBoolean(name = "cf_lost_enabled", defaultValue = false)
    val codeforcesLostMinRatingTag = itemEnum(name = "cf_lost_min_rating", defaultValue = CodeforcesColorTag.ORANGE)

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

    enum class NewsFeed {
        atcoder_news,
        project_euler_news,
        project_euler_problems
    }

    val enabledNewsFeeds = itemEnumSet<NewsFeed>(name = "news_feeds")

    val renderAllTabs = itemBoolean(name = "tabs_render_all", defaultValue = true)
}