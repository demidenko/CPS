package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.demich.cps.platforms.api.CodeforcesApiException
import com.demich.cps.platforms.api.isResponseException
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.joinAllWithCounter
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class CPSWorker(
    protected val work: CPSPeriodicWork,
    val parameters: WorkerParameters
): CoroutineWorker(work.context, parameters) {

    protected val context get() = work.context
    protected val workerStartTime by lazy { getCurrentTime() }

    final override suspend fun doWork(): Result {
        if (!work.isEnabled()) {
            work.stop()
            return Result.success()
        }

        val workersInfo = CPSWorkersDataStore(context)

        val result = withContext(Dispatchers.IO) {
            workersInfo.lastExecutions.edit {
                this[work.name] = ExecutionEvent(start = workerStartTime)
            }

            smartRunWork().also { result ->
                workersInfo.lastExecutions.edit {
                    this[work.name] = ExecutionEvent(
                        start = workerStartTime,
                        end = getCurrentTime(),
                        resultType = result.toType()
                    )
                }
                if (result.toType() == ResultType.RETRY) {
                    work.enqueueAsap()
                }
            }
        }

        return result
    }

    protected abstract suspend fun runWork(): Result
    private suspend fun smartRunWork(): Result {
        suspend fun call(): Result =
            runCatching { runWork() }.getOrElse {
                when {
                    it.isResponseException -> Result.retry()
                    it is CodeforcesApiException -> Result.retry()
                    else -> Result.failure()
                }
            }

        return call().let { result ->
            if (result.toType() != ResultType.SUCCESS) {
                delay(duration = 5.seconds)
                call()
            } else result
        }
    }

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
        if (isEmpty()) return
        joinAllWithCounter {
            setProgressInfo(ProgressBarInfo(current = it, total = size))
        }
    }


    enum class ResultType {
        SUCCESS, RETRY, FAILURE
    }

    private fun Result.toType(): ResultType? {
        return when (this) {
            is Result.Success -> ResultType.SUCCESS
            is Result.Retry -> ResultType.RETRY
            is Result.Failure -> ResultType.FAILURE
            else -> null
        }
    }

    @Serializable
    data class ExecutionEvent(
        val start: Instant,
        val end: Instant? = null,
        val resultType: ResultType? = null
    ) {
        val duration: Duration? get() = end?.minus(start)
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

    //val executions = jsonCPS.itemMap<String, List<CPSWorker.ExecutionEvent>>("executions")
    val lastExecutions = jsonCPS.itemMap<String, CPSWorker.ExecutionEvent>("last_executions")
}
