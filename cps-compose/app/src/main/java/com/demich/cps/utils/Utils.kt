package com.demich.cps.utils

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.text.HtmlCompat
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun getCurrentTime() = Clock.System.now()

fun<T> T.touchLog(text: String) = also {
    println("${getCurrentTime().epochSeconds}: $text")
}

val context: Context
    @Composable
    @ReadOnlyComposable
    get() = LocalContext.current

fun Context.showToast(title: String) = Toast.makeText(this, title, Toast.LENGTH_LONG).show()

val jsonCPS = Json { ignoreUnknownKeys = true }

fun cpsHttpClient(block: HttpClientConfig<*>.() -> Unit) = HttpClient {
    install(HttpTimeout) {
        connectTimeoutMillis = 15.seconds.inWholeMilliseconds
        requestTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
    install(JsonFeature) {
        serializer = KotlinxSerializer(jsonCPS)
    }
    block()
}

fun signedToString(x: Int): String = if (x > 0) "+$x" else "$x"

fun Instant.format(dateFormat: String): String =
    DateFormat.format(dateFormat, toEpochMilliseconds()).toString()

fun fromHTML(s: String): Spanned {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(s)
    }
}

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

enum class LoadingStatus {
    PENDING, LOADING, FAILED;
}

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

inline fun<reified T> jsonSaver() = object: Saver<T, String> {
    override fun restore(value: String): T = jsonCPS.decodeFromString(value)
    override fun SaverScope.save(value: T): String = jsonCPS.encodeToString(value)
}

@Composable
fun<T> rememberCollect(block: () -> Flow<T>) =
    remember { block() }.let {
        it.collectAsState(initial = remember(it) { runBlocking { it.first() } })
    }