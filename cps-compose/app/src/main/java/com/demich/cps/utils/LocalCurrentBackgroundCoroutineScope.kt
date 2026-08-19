package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val LocalCurrentBackgroundCoroutineScope = staticCompositionLocalOf<CoroutineScope> {
    throw IllegalStateException("no background coroutine scope")
}

val backgroundCoroutineScope: CoroutineScope
    @Composable
    @ReadOnlyComposable
    inline get() = LocalCurrentBackgroundCoroutineScope.current

class BackgroundJobsViewModel: ViewModel(
    viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
)

@Composable
fun ProvideBackgroundCoroutineScope(content: @Composable () -> Unit) {
    val viewModel = sharedViewModel<BackgroundJobsViewModel>()
    CompositionLocalProvider(
        LocalCurrentBackgroundCoroutineScope provides viewModel.viewModelScope,
        content = content
    )
}