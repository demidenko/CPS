package com.demich.cps.ui.bottomprogressbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.utils.set
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
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

    val flowOfProgresses: StateFlow<Map<String, ProgressBarInfo>>
        field = MutableStateFlow(emptyMap())

    fun doJob(
        id: String,
        coroutineScope: CoroutineScope = viewModelScope,
        block: suspend ProducerScope<ProgressBarInfo>.() -> Unit
    ) {
        coroutineScope.launch(Dispatchers.Default) {
            channelFlow {
                block()
                // compose doest not catch fast changes so this delay is necessary
                delay(1.seconds)
                send(null)
            }.collect { value ->
                flowOfProgresses.set(key = id, value = value)
            }
        }
    }
}