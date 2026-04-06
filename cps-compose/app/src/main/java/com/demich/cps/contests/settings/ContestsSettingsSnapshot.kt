package com.demich.cps.contests.settings

import com.demich.cps.contests.database.ContestPlatform
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.serialization.Serializable

@Serializable
class ContestsSettingsSnapshot(
    val enabledPlatforms: Set<ContestPlatform>,
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
    val toReload: Set<ContestPlatform>,
    val toRemove: Set<ContestPlatform>
)

fun ContestsSettingsSnapshot.differenceFrom(snapshot: ContestsSettingsSnapshot): ContestsSettingsSnapshotDiff {
    val toReload = mutableSetOf<ContestPlatform>()
    val toRemove: Set<ContestPlatform>

    snapshot.enabledPlatforms.let { prev ->
        val current = enabledPlatforms
        toRemove = prev - current
        toReload.addAll(current - prev)
    }

    snapshot.clistResourcesIds.let { prev ->
        val current = clistResourcesIds
        if (prev != current) {
            toReload.add(unknown)
        }
    }

    snapshot.contestsDateConstraints.let { prev ->
        val current = contestsDateConstraints
        if (prev != current) {
            //TODO: delete contests if current in prev
            toReload.addAll(ContestPlatform.entries)
        }
    }

    return ContestsSettingsSnapshotDiff(
        toReload = toReload.intersect(enabledPlatforms),
        toRemove = toRemove
    )
}