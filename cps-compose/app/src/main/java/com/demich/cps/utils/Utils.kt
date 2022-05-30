package com.demich.cps.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun getCurrentTime() = Clock.System.now()

fun<T> T.touchLog(text: String) = also {
    println("${getCurrentTime().epochSeconds}: $text")
}

fun Context.showToast(title: String) = Toast.makeText(this, title, Toast.LENGTH_LONG).show()

fun Context.openUrlInBrowser(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

val jsonCPS = Json {
    ignoreUnknownKeys = true
}

fun cpsHttpClient(
    json: Boolean = true,
    block: HttpClientConfig<*>.() -> Unit
) = HttpClient {
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = 15.seconds.inWholeMilliseconds
        requestTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
    if (json) {
        install(ContentNegotiation) {
            json(json = jsonCPS)
        }
    }
    block()
}

suspend inline fun<reified T> HttpClient.getAs(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = this.get(urlString = urlString, block = block).body()

suspend inline fun HttpClient.getText(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): String = this.get(urlString = urlString, block = block).bodyAsText()


fun signedToString(x: Int): String = if (x > 0) "+$x" else "$x"


fun Instant.format(dateFormat: String): String =
    DateFormat.format(dateFormat, toEpochMilliseconds()).toString()

fun Duration.toHHMMSS(): String = toComponents { hours, minutes, seconds, _ ->
    String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun timeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    return when {
        t < 2.minutes -> "minute"
        t < 2.hours -> "${t.inWholeMinutes} minutes"
        t < 24.hours * 2 -> "${t.inWholeHours} hours"
        t < 7.days * 2 -> "${t.inWholeDays} days"
        t < 31.days * 2 -> "${t.inWholeDays / 7} weeks"
        t < 365.days * 2 -> "${t.inWholeDays / 31} months"
        else -> "${t.inWholeDays / 365} years"
    }
}

fun timeAgo(fromTime: Instant, toTime: Instant) = timeDifference(fromTime, toTime) + " ago"


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

enum class LoadingStatus {
    PENDING, LOADING, FAILED;
}

fun Iterable<LoadingStatus>.combine(): LoadingStatus
    = when {
        contains(LoadingStatus.LOADING) -> LoadingStatus.LOADING
        contains(LoadingStatus.FAILED) -> LoadingStatus.FAILED
        else -> LoadingStatus.PENDING
    }

fun Iterable<State<LoadingStatus>>.combine(): State<LoadingStatus>
    = derivedStateOf { map { it.value }.combine() }

object InstantAsSecondsSerializer: KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.epochSeconds)
    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochSeconds(decoder.decodeLong())
}

object DurationAsSecondsSerializer: KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.inWholeSeconds)
    override fun deserialize(decoder: Decoder): Duration = decoder.decodeLong().seconds
}
