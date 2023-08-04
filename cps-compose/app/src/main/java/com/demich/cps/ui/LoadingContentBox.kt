package com.demich.cps.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus


//TODO: try again request button (?)

@Composable
fun LoadingContentBox(
    modifier: Modifier = Modifier,
    loadingStatus: LoadingStatus,
    failedText: String,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (loadingStatus) {
            LoadingStatus.PENDING -> content()
            LoadingStatus.LOADING -> LoadingIndicator()
            LoadingStatus.FAILED -> FailedText(failedText)
        }
    }
}


@Composable
fun<T> LoadingContentBox(
    modifier: Modifier = Modifier,
    data: Result<T>?,
    failedText: (Throwable) -> String,
    content: @Composable (T) -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (data == null) {
            LoadingIndicator()
        } else {
            data.onSuccess {
                content(it)
            }.onFailure {
                FailedText(failedText(it))
            }
        }
    }
}

@Composable
private fun LoadingIndicator() =
    CircularProgressIndicator(
        color = cpsColors.content,
        strokeWidth = 3.dp
    )

@Composable
private fun FailedText(failedText: String) =
    Text(
        text = failedText,
        color = cpsColors.error,
        fontWeight = FontWeight.SemiBold
    )