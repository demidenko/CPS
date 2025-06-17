package com.demich.cps.features.room

import kotlinx.serialization.json.Json

interface RoomJsonConverter<T> {
    fun encode(value: T): String
    fun decode(str: String): T
}

private val defaultJsonRoom = Json {
    encodeDefaults = true
    allowStructuredMapKeys = true
    ignoreUnknownKeys = true
}

val RoomJsonConverter<*>.jsonRoom get() = defaultJsonRoom