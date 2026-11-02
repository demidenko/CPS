package com.demich.cps.community

import android.content.Context
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.CommunitySettingsDataStore
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesFollowRepository
import com.demich.cps.workers.CodeforcesLostDataStore

interface CommunityUnitsFactory {
    fun settingsStorage(): CommunitySettingsDataStore

    fun followRepository(): CodeforcesFollowRepository

    fun lostStorage(): CodeforcesLostDataStore
}

@JvmInline
private value class CommunityUnitsAndroidFactory(val context: Context): CommunityUnitsFactory {
    override fun settingsStorage() = context.settingsCommunity

    override fun followRepository() = context.followRepository

    override fun lostStorage() = CodeforcesLostDataStore(context)
}

val LocalCommunityUnitsFactory = compositionLocalWithComputedDefaultOf<CommunityUnitsFactory> {
    CommunityUnitsAndroidFactory(context = LocalContext.currentValue)
}