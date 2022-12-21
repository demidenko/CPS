package com.demich.cps.workers

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.edit
import kotlinx.datetime.Instant

abstract class CPSWorker(
    private val work: CPSWork,
    val parameters: WorkerParameters
): CoroutineWorker(work.context, parameters) {

    protected val context get() = work.context
    protected val currentTime by lazy { getCurrentTime() }

    final override suspend fun doWork(): Result {
        if (!work.isEnabled()) {
            work.stop()
            return Result.success()
        }

        CPSWorkersDataStore(context).lastExecutionTime.edit {
            this[work.name] = currentTime
        }

        return runWork()
    }

    abstract suspend fun runWork(): Result

    protected suspend fun setProgressInfo(progressInfo: ProgressBarInfo) {
        if (progressInfo.total == 0) return
        setProgress(workDataOf(
            KEY_PROGRESS to arrayOf(progressInfo.current, progressInfo.total)
        ))
    }

    protected suspend inline fun<reified T> List<T>.forEachWithProgress(
        action: (T) -> Unit
    ) {
        var progressInfo = ProgressBarInfo(total = size)
        setProgressInfo(progressInfo)
        forEach {
            action(it)
            progressInfo++
            setProgressInfo(progressInfo)
        }
    }

}

private val KEY_PROGRESS get() = "cpsworker_progress"
fun WorkInfo.getProgressInfo(): ProgressBarInfo? {
    val arr = progress.getIntArray(KEY_PROGRESS)?.takeIf { it.size == 2 } ?: return null
    return ProgressBarInfo(
        current = arr[0],
        total = arr[1]
    )
}


class CPSWorkersDataStore(context: Context): ItemizedDataStore(context.workersDataStore) {
    companion object {
        private val Context.workersDataStore by preferencesDataStore(name = "workers_info")
    }

    val lastExecutionTime = jsonCPS.item<Map<String, Instant>>(name = "last_execution_time", defaultValue = emptyMap())
}
