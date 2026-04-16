package com.demich.cps.contests.settings

import com.demich.cps.platforms.Platform
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.serialization.Serializable

@Serializable
class ContestsSettingsSnapshot(
    val enabledPlatforms: Set<Platform>,
    val clistResourcesIds: Set<Int>,
    val contestsDateConstraints: ContestDateRelativeConstraints
)

suspend fun ContestsSettingsDataStore.makeSnapshot(): ContestsSettingsSnapshot =
    fromSnapshot {
        ContestsSettingsSnapshot(
            enabledPlatforms = enabledPlatforms.value,
            clistResourcesIds = clistAdditionalResources.value.mapToSet { it.id },
            contestsDateConstraints = contestsDateConstraints.value
        )
    }


class ContestsSettingsSnapshotDiff(
    val toReload: Set<Platform>,
    val toRemove: Set<Platform>,
    val clistReload: Boolean
)

fun ContestsSettingsSnapshot.differenceFrom(snapshot: ContestsSettingsSnapshot): ContestsSettingsSnapshotDiff {
    val toReload = mutableSetOf<Platform>()
    val toRemove: Set<Platform>

    snapshot.enabledPlatforms.let { prev ->
        val current = enabledPlatforms
        toRemove = prev - current
        toReload.addAll(current - prev)
    }

    var clistReload = snapshot.clistResourcesIds.let { prev ->
        val current = clistResourcesIds
        prev != current
    }

    snapshot.contestsDateConstraints.let { prev ->
        val current = contestsDateConstraints
        if (prev != current) {
            //TODO: delete contests if current in prev
            toReload.addAll(contestPlatforms)
            clistReload = true
        }
    }

    return ContestsSettingsSnapshotDiff(
        toReload = toReload.intersect(enabledPlatforms),
        toRemove = toRemove,
        clistReload = clistReload
    )
}