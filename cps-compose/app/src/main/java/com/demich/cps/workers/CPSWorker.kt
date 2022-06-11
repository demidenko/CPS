package com.demich.cps.workers

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

abstract class CPSWorker(
    private val work: CPSWork,
    val parameters: WorkerParameters
): CoroutineWorker(work.context, parameters) {

    protected val context get() = work.context

    final override suspend fun doWork(): Result {
        if (!work.isEnabled()) {
            work.stop()
            return Result.success()
        }
        return runWork()
    }

    abstract suspend fun runWork(): Result

}

