package com.demich.cps.workers

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

abstract class CPSWorker(
    val carrier: WorkerCarrier,
    val parameters: WorkerParameters
): CoroutineWorker(carrier.context, parameters) {

    protected val context get() = carrier.context

    final override suspend fun doWork(): Result {
        if (!carrier.isEnabled()) {
            carrier.stop()
            return Result.success()
        }
        return runWork()
    }

    abstract suspend fun runWork(): Result

}

