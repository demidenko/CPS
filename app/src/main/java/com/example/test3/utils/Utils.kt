package com.example.test3.utils

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.widget.EditText
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration

fun getCurrentTimeSeconds() = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

fun getColorFromResource(context: Context, resourceId: Int): Int {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        context.resources.getColor(resourceId, null)
    } else {
        context.resources.getColor(resourceId)
    }
}

val httpClient = OkHttpClient
    .Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

val jsonCPS = Json{ ignoreUnknownKeys = true }
val jsonConverterFactory = jsonCPS.asConverterFactory(MediaType.get("application/json"))

fun fromHTML(s: String): Spanned {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(s)
    }
}

fun signedToString(x: Int): String = if(x>0) "+$x" else "$x"

fun durationHHMMSS(duration: Duration) : String =
    duration.toComponents { hours, minutes, seconds, _ ->
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

class MutableSetLiveSize<T>() {
    private val s = mutableSetOf<T>()
    private val size = MutableStateFlow<Int>(0)
    val sizeStateFlow get() = size.asStateFlow()

    fun values() = s.toSet()

    operator fun contains(element: T) = s.contains(element)

    fun add(element: T) = s.add(element).also { size.value = s.size }
    fun addAll(elements: Collection<T>) = s.addAll(elements).also { size.value = s.size }

    fun remove(element: T) = s.remove(element).also { size.value = s.size }
    fun removeAll(elements: Collection<T>) = s.removeAll(elements).also { size.value = s.size }

    fun clear() {
        s.clear()
        size.value = 0
    }
}


enum class LoadingState {
    PENDING, LOADING, FAILED;

    companion object {
        fun combineLoadingStateFlows(states: List<Flow<LoadingState>>): Flow<LoadingState> =
            combine(states){
                when {
                    it.contains(LOADING) -> LOADING
                    it.contains(FAILED) -> FAILED
                    else -> PENDING
                }
            }
    }
}

enum class BlockedState {
    BLOCKED, UNBLOCKED;

    companion object {
        fun combineBlockedStatesFlows(states: List<Flow<BlockedState>>): Flow<BlockedState> =
            combine(states){
                if(it.contains(BLOCKED)) BLOCKED
                else UNBLOCKED
            }
    }
}

suspend inline fun<reified A, reified B> asyncPair(
    crossinline getA: suspend () -> A,
    crossinline getB: suspend () -> B,
): Pair<A, B> {
    return coroutineScope {
        val a = async { getA() }
        val b = async { getB() }
        Pair(a.await(), b.await())
    }
}

data class ComparablePair<A: Comparable<A>, B: Comparable<B>>(
    val first: A,
    val second: B
): Comparable<ComparablePair<A, B>> {
    override fun compareTo(other: ComparablePair<A, B>): Int {
        val c = first compareTo other.first
        return if(c == 0) second compareTo other.second else c
    }
}

fun<T> Array<out T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    for(i in 1 until size) if(comparator.compare(get(i-1),get(i))>0) return false
    return true
}

fun<T> Flow<T>.ignoreFirst(): Flow<T> {
    var ignore = true
    return transform { value ->
        if(!ignore) emit(value)
        else ignore = false
    }
}

suspend fun<T> collectionsDifference(
    new: Collection<T>,
    old: Collection<T>,
    callback: suspend (added: Collection<T>, removed: Collection<T>) -> Unit
) {
    callback(
        new.filter { it !in old },
        old.filter { it !in new }
    )
}

inline fun<reified T: Any> classDifference(a: T, b: T): List<KProperty1<T, *>> {
    return T::class.memberProperties.filter {
        it.get(a) != it.get(b)
    }
}

inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

fun EditText.getStringNotBlank(): String? = text?.toString()?.takeIf { it.isNotBlank() }

suspend inline fun CoroutineScope.startTimer(delay: Duration, crossinline action: suspend () -> Unit) {
    require(delay != Duration.ZERO)
    while (isActive) {
        action()
        delay(delay.inWholeMilliseconds)
    }
}