package com.demich.cps.workers

import android.content.Context
import com.demich.cps.community.CommunityNewsFeed
import com.demich.cps.platforms.utils.NewsPostEntry
import com.demich.cps.platforms.utils.scanNewsPostEntries
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit


class WorkersHintsDataStore(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by dataStoreWrapper(name = "workers_hints")
    }

    val newsFeedsLastIds = jsonCPS.itemMap<CommunityNewsFeed, String>(name = "news_feeds_last_id")
}

// TODO: pass work provider instead of provider?
fun workerDataStoreDelegate(provider: CPSPeriodicWorkProvider) =
    dataStoreWrapper(name = "WORKER_${provider.workName}_storage")

suspend inline fun <T: NewsPostEntry> List<T>.scanNewsFeed(
    newsFeed: CommunityNewsFeed,
    hintsDataStore: WorkersHintsDataStore,
    onNewPost: (T) -> Unit
) {
    val item = hintsDataStore.newsFeedsLastIds
    scanNewsPostEntries(
        getLastId = {
            item()[newsFeed]
        },
        setLastId = {
            item.edit { set(key = newsFeed, value = it) }
        },
        onNewPost = onNewPost
    )
}