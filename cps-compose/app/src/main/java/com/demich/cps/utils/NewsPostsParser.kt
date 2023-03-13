package com.demich.cps.utils

interface NewsPostEntry {
    val id: String
}

suspend fun<T: NewsPostEntry, E> scanNewsPostEntries(
    elements: List<E>,
    getLastId: suspend () -> String?,
    setLastId: suspend (String) -> Unit,
    extractPost: (E) -> T?,
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