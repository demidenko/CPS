package com.demich.cps.features.codeforces.lost.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demich.cps.platforms.api.InstantAsSecondsSerializer
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Entity(tableName = cfLostTableName)
internal data class CodeforcesLostBlogEntry(
    val blogEntry: DeprecatedCodeforcesBlogEntry,

    @PrimaryKey
    val id: Int = blogEntry.id,

    val isSuspect: Boolean,
    val isDeleted: Boolean = false,
    val timeStamp: Instant
)

@Serializable
internal data class DeprecatedCodeforcesBlogEntry(
    val id: Int,

    val title: String, //already parsed

    val authorHandle: String,

    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,

    val rating: Int,

    val authorColorTag: CodeforcesColorTag
)

internal fun DeprecatedCodeforcesBlogEntry.toWebBlogEntry() =
    CodeforcesWebBlogEntry(
        id = id,
        title = title,
        author = CodeforcesHandle(handle = authorHandle, colorTag = authorColorTag),
        creationTime = creationTime,
        rating = rating,
        commentsCount = 0
    )

internal fun CodeforcesWebBlogEntry.toDeprecatedBlogEntry() =
    DeprecatedCodeforcesBlogEntry(
        id = id,
        title = title,
        authorHandle = author.handle,
        creationTime = creationTime,
        rating = rating,
        authorColorTag = author.colorTag
    )