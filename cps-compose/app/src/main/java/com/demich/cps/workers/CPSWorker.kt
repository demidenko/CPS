package com.demich.cps.workers

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.edit
import com.demich.cps.utils.getCurrentTime
import kotlinx.datetime.Instant

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

        CPSWorkersDataStore(context).lastExecutionTime.edit {
            this[work.name] = getCurrentTime()
        }

        return runWork()
    }

    abstract suspend fun runWork(): Result

}


class CPSWorkersDataStore(context: Context): CPSDataStore(context.workersDataStore) {
    companion object {
        private val Context.workersDataStore by preferencesDataStore(name = "workers_info")
    }

    val lastExecutionTime = itemJsonable<Map<String, Instant>>(name = "last_execution_time", defaultValue = emptyMap())
}
