package com.demich.cps.features.room

import kotlinx.serialization.json.Json

abstract class RoomJsonConverter<T> {
    protected val json = defaultJsonRoom()

    abstract fun encode(value: T): String

    abstract fun decode(str: String): T

    protected inline fun <reified T> decodeFromString(string: String): T =
        json.decodeFromString(string = string)

    protected inline fun <reified T> encodeToString(value: T): String =
        json.encodeToString(value = value)
}

private fun defaultJsonRoom() = Json {
    encodeDefaults = true
    allowStructuredMapKeys = true
    ignoreUnknownKeys = true
}