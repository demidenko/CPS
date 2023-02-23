package com.demich.cps.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun<T> T.touchLog(text: String) = also {
    println("${getCurrentTime().epochSeconds}: $text")
}

fun Context.showToast(title: String) = Toast.makeText(this, title, Toast.LENGTH_LONG).show()

fun Context.openUrlInBrowser(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

val jsonCPS = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
}



fun Int.toSignedString(): String = if (this > 0) "+${this}" else "$this"


data class ComparablePair<A: Comparable<A>, B: Comparable<B>>(
    val first: A,
    val second: B
): Comparable<ComparablePair<A, B>> {
    override fun compareTo(other: ComparablePair<A, B>): Int =
        when (val c = first compareTo other.first) {
            0 -> second compareTo other.second
            else -> c
        }
}

fun <T> List<T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    if (size < 2) return true
    for (i in 1 until size) if (comparator.compare(get(i-1),get(i)) > 0) return false
    return true
}

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> = mapTo(mutableSetOf(), transform)


fun<K, V> Map<K, Flow<V>>.combine(): Flow<Map<K, V>> =
    combine(entries.map { (key, value) -> value.map { key to it } }) { it.toMap() }

suspend fun<A, B> awaitPair(
    context: CoroutineContext = EmptyCoroutineContext,
    blockFirst: suspend CoroutineScope.() -> A,
    blockSecond: suspend CoroutineScope.() -> B,
): Pair<A, B> {
    return coroutineScope {
        val first = async(context = context, block = blockFirst)
        val second = async(context = context, block = blockSecond)
        Pair(first.await(), second.await())
    }
}
