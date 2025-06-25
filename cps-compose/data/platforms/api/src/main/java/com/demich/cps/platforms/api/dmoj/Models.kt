package com.demich.cps.platforms.api.dmoj

import kotlinx.serialization.Serializable

@Serializable
data class DmojUserResult(
    val id: Int,
    val username: String,
    val rating: Int?
)

@Serializable
data class DmojSuggestion(
    val text: String,
    val id: String
)

@Serializable
data class DmojRatingChange(
    val label: String,
    val rating: Int,
    val ranking: Int,
    val link: String,
    val timestamp: Double
)

@Serializable
data class DmojContest(
    val key: String,
    val name: String,

    //contest start time in ISO format
    val start_time: String,

    //contest end time in ISO format
    val end_time: String,

    //contest time limit in seconds, or null if the contest is not windowed
    //Double because of "time_limit":10800.0
    val time_limit: Double?
)