package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        val result = withContext(Dispatchers.IO) {
            runWork()
        }

        return result
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

    protected suspend fun List<suspend () -> Unit>.joinAllWithProgress() {
        val progressStateFlow = MutableStateFlow(ProgressBarInfo(total = size))
        withContext(Dispatchers.IO) {
            progressStateFlow.transformWhile {
                emit(it)
                it.current != it.total
            }.onEach(::setProgressInfo).launchIn(this)

            map { job ->
                launch {
                    try {
                        job()
                    } finally {
                        progressStateFlow.update { it.inc() }
                    }
                }
            }.joinAll()
        }
    }

}

private const val KEY_PROGRESS = "cpsworker_progress"
fun WorkInfo.getProgressInfo(): ProgressBarInfo? {
    val arr = progress.getIntArray(KEY_PROGRESS)?.takeIf { it.size == 2 } ?: return null
    return ProgressBarInfo(
        current = arr[0],
        total = arr[1]
    )
}


class CPSWorkersDataStore(context: Context): ItemizedDataStore(context.workersDataStore) {
    companion object {
        private val Context.workersDataStore by dataStoreWrapper(name = "workers_info")
    }

    val lastExecutionTime = jsonCPS.itemMap<String, Instant>(name = "last_execution_time")
}
