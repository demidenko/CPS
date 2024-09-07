package com.demich.cps.community.settings

import android.content.Context
import com.demich.cps.community.codeforces.CodeforcesTitle
import com.demich.cps.utils.jsonCPS
import com.demich.cps.platforms.api.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.CodeforcesLocale
import com.demich.cps.platforms.utils.NewsPostEntry
import com.demich.cps.platforms.utils.scanNewsPostEntries
import com.demich.cps.utils.isRuSystemLanguage
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

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
    val codeforcesLostHintNotNew = jsonCPS.item<Pair<Int,Instant>?>(name = "cf_lost_hint", defaultValue = null)

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
        ;

        val shortName: String get() = when (this) {
            atcoder_news -> "atcoder"
            project_euler_news -> "pe_news"
            project_euler_problems -> "pe_problems"
        }

        val link: String get() = when (this) {
            atcoder_news -> "atcoder.jp"
            project_euler_news -> "projecteuler.net/news"
            project_euler_problems -> "projecteuler.net/recent"
        }
    }

    val enabledNewsFeeds = itemEnumSet<NewsFeed>(name = "news_feeds")
    val newsFeedsLastIds = jsonCPS.itemMap<NewsFeed,String>(name = "news_feeds_last_id")

    suspend fun<T: NewsPostEntry> scanNewsFeed(
        newsFeed: NewsFeed,
        posts: List<T?>,
        onNewPost: (T) -> Unit
    ) {
        scanNewsPostEntries(
            posts = posts,
            onNewPost = onNewPost,
            getLastId = {
                newsFeedsLastIds()[newsFeed]
            },
            setLastId = {
                newsFeedsLastIds.edit { this[newsFeed] = it }
            }
        )
    }
}