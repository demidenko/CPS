package com.demich.cps.features.codeforces.lost.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import kotlinx.datetime.Instant

@Entity(tableName = cfLostTableName)
data class CodeforcesLostBlogEntry(
    val blogEntry: CodeforcesBlogEntry,

    @PrimaryKey
    val id: Int = blogEntry.id,

    val isSuspect: Boolean,
    val isDeleted: Boolean = false,
    val timeStamp: Instant
)