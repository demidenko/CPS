package com.demich.cps.platforms.utils

interface NewsPostEntry {
    val id: String
}

suspend fun<T: NewsPostEntry> scanNewsPostEntries(
    posts: List<T?>,
    getLastId: suspend () -> String?,
    setLastId: suspend (String) -> Unit,
    onNewPost: (T) -> Unit
) {
    val lastId = getLastId()

    val newEntries = posts
        .filterNotNull()
        .takeWhile { it.id != lastId }

    if (newEntries.isEmpty()) return

    if (lastId != null) {
        newEntries.forEach(onNewPost)
    }

    setLastId(newEntries.first().id)
}