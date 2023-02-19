package com.demich.cps.workers

import org.jsoup.nodes.Element
import org.jsoup.select.Elements

internal interface PostEntry {
    val id: String
}

internal suspend fun<T: PostEntry> getPosts(
    elements: Elements,
    getLastId: suspend () -> String?,
    setLastId: suspend (String) -> Unit,
    extractPost: (Element) -> T?,
    onNewPost: (T) -> Unit
) {
    val lastId = getLastId()

    val newEntries = buildList {
        for (element in elements) {
            val post = extractPost(element) ?: continue
            if (post.id == lastId) break
            add(post)
        }
    }

    if (newEntries.isEmpty()) return

    if (lastId != null) {
        newEntries.forEach(onNewPost)
    }

    setLastId(newEntries.first().id)
}