package com.demich.cps.contests

import android.content.Context
import com.demich.cps.contests.database.ContestsRepository
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.workers.CPSPeriodicWork
import com.demich.cps.workers.ContestsWorker

interface ContestsUnitsFactory {
    fun repository(): ContestsRepository

    fun settingsStorage(): ContestsSettingsDataStore

    fun infoStorage(): ContestsInfoDataStore

    fun codeforcesMonitorStorage(): CodeforcesMonitorDataStore

    fun work(): CPSPeriodicWork
}

class ContestsUnitsAndroidFactory(val context: Context): ContestsUnitsFactory {
    override fun repository() = context.contestsRepository

    override fun settingsStorage() = context.settingsContests

    override fun infoStorage() = ContestsInfoDataStore(context)

    override fun codeforcesMonitorStorage() = CodeforcesMonitorDataStore(context)

    override fun work() = ContestsWorker.getWork(context)
}
