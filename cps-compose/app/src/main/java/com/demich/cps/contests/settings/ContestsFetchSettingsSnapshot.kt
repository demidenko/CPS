package com.demich.cps.contests.settings

import com.demich.cps.platforms.Platform
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.serialization.Serializable

@Serializable
class ContestsFetchSettingsSnapshot(
    val enabledPlatforms: Set<Platform>,
    val clistResourcesIds: Set<Int>,
    val contestsDateConstraints: ContestDateRelativeConstraints
)

suspend fun ContestsSettingsDataStore.fetchSettingsSnapshot(): ContestsFetchSettingsSnapshot =
    fromSnapshot {
        ContestsFetchSettingsSnapshot(
            enabledPlatforms = enabledPlatforms.value,
            clistResourcesIds = clistAdditionalResources.value.mapToSet { it.id },
            contestsDateConstraints = contestsDateConstraints.value
        )
    }


class ContestsFetchSettingsSnapshotDiff(
    val toReload: Set<Platform>,
    val toRemove: Set<Platform>,
    val clistReload: Boolean
)

fun ContestsFetchSettingsSnapshot.differenceFrom(other: ContestsFetchSettingsSnapshot): ContestsFetchSettingsSnapshotDiff {
    val toReload = mutableSetOf<Platform>()
    val toRemove: Set<Platform>

    other.enabledPlatforms.let { prev ->
        val current = enabledPlatforms
        toRemove = prev - current
        toReload.addAll(current - prev)
    }

    var clistReload = other.clistResourcesIds.let { prev ->
        val current = clistResourcesIds
        prev != current
    }

    other.contestsDateConstraints.let { prev ->
        val current = contestsDateConstraints
        if (prev != current) {
            //TODO: delete contests if current in prev
            toReload.addAll(contestPlatforms)
            clistReload = true
        }
    }

    return ContestsFetchSettingsSnapshotDiff(
        toReload = toReload.intersect(enabledPlatforms),
        toRemove = toRemove,
        clistReload = clistReload
    )
}