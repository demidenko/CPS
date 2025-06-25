package com.demich.cps.platforms.api.clist

import kotlinx.serialization.Serializable


@Serializable
data class ClistContest(
    val resource_id: Int,
    val id: Long,
    val start: String,
    val end: String,
    val duration: Long,
    val event: String,
    val href: String,
    val host: String
)

@Serializable
data class ClistResource(
    val id: Int,
    val name: String
)