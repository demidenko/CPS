package com.demich.cps.platforms.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


object InstantAsSecondsSerializer: KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Instant - seconds", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.epochSeconds)
    override fun deserialize(decoder: Decoder): Instant =
        Instant.fromEpochSeconds(decoder.decodeLong())
}

object DurationAsSecondsSerializer: KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Duration - seconds", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.inWholeSeconds)
    override fun deserialize(decoder: Decoder): Duration = decoder.decodeLong().seconds
}