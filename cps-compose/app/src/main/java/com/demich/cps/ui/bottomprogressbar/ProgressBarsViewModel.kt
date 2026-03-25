package com.demich.cps.ui.bottomprogressbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.utils.edit
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@Immutable
data class ProgressBarInfo(
    val total: Int,
    val current: Int = 0,
    val title: String = ""
) {
    val fraction: Float get() = current.toFloat() / total

    operator fun inc(): ProgressBarInfo = copy(current = current + 1)
}

@Composable
fun progressBarsViewModel(): ProgressBarsViewModel = sharedViewModel()

class ProgressBarsViewModel: ViewModel() {
    private val progressesStateFlow = MutableStateFlow(emptyMap<String, ProgressBarInfo>())
    fun flowOfProgresses(): StateFlow<Map<String, ProgressBarInfo>> = progressesStateFlow

    fun doJob(
        id: String,
        coroutineScope: CoroutineScope = viewModelScope,
        block: suspend FlowCollector<ProgressBarInfo>.() -> Unit
    ) {
        coroutineScope.launch(Dispatchers.Default) {
            flow {
                block()
                // compose doest not catch fast changes so this delay is necessary
                delay(1.seconds)
                emit(null)
            }.collect { value ->
                progressesStateFlow.edit {
                    if (value == null) remove(key = id)
                    else put(key = id, value = value)
                }
            }
        }
    }

    fun flowOfClistImportIsRunning(): Flow<Boolean> =
        progressesStateFlow.map { clistImportId in it }

    companion object {
        const val clistImportId = "clist_import"
    }
}