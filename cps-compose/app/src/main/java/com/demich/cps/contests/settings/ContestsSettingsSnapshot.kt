package com.demich.cps.contests.settings

import com.demich.cps.contests.database.Contest
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


class ContestsSettingsSnapshotDiff(
    val toReload: Set<Contest.Platform>,
    val toRemove: Set<Contest.Platform>
)

fun ContestsSettingsSnapshot.differenceFrom(snapshot: ContestsSettingsSnapshot): ContestsSettingsSnapshotDiff {
    val toReload = mutableSetOf<Contest.Platform>()
    val toRemove: Set<Contest.Platform>

    snapshot.enabledPlatforms.let { prev ->
        val current = enabledPlatforms
        toRemove = prev - current
        toReload.addAll(current - prev)
    }

    snapshot.clistAdditionalResources.let { prev ->
        val current = clistAdditionalResources
        if (prev != current) {
            toReload.add(Contest.Platform.unknown)
        }
    }

    snapshot.contestsDateConstraints.let { prev ->
        val current = contestsDateConstraints
        if (prev != current) {
            //TODO: delete contests if current in prev
            toReload.addAll(Contest.platforms)
        }
    }

    return ContestsSettingsSnapshotDiff(
        toReload = toReload.intersect(enabledPlatforms),
        toRemove = toRemove
    )
}