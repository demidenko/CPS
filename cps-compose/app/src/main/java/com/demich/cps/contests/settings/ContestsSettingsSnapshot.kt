package com.demich.cps.contests.settings

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateBaseConstraints
import com.demich.datastore_itemized.fromSnapshot
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.serialization.Serializable

@Serializable
class ContestsSettingsSnapshot(
    val enabledPlatforms: Set<Contest.Platform>,
    val clistAdditionalResources: Set<Int>,
    val contestsDateConstraints: ContestDateBaseConstraints
)

suspend fun ContestsSettingsDataStore.makeSnapshot(): ContestsSettingsSnapshot =
    fromSnapshot {
        ContestsSettingsSnapshot(
            enabledPlatforms = enabledPlatforms(it),
            clistAdditionalResources = it[clistAdditionalResources].mapToSet { it.id },
            contestsDateConstraints = it[contestsDateConstraints]
        )
    }