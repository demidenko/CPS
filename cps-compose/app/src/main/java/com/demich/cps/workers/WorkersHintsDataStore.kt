package com.demich.cps.workers

import android.content.Context
import com.demich.cps.community.settings.CommunitySettingsDataStore.NewsFeed
import com.demich.cps.platforms.utils.NewsPostEntry
import com.demich.cps.platforms.utils.scanNewsPostEntries
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlin.time.Instant


class WorkersHintsDataStore(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by dataStoreWrapper(name = "workers_hints")
    }

    val followLastSuccessTime = jsonCPS.itemNullable<Instant>(name = "follow_last_success")

    val codeforcesLostHintNotNew = jsonCPS.itemNullable<CodeforcesLostHint>(name = "cf_lost_hint")

    val newsFeedsLastIds = jsonCPS.itemMap<NewsFeed,String>(name = "news_feeds_last_id")
}

suspend inline fun <T: NewsPostEntry> WorkersHintsDataStore.scanNewsFeed(
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