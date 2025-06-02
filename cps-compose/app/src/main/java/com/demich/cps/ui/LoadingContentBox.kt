package com.demich.cps.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ProvideContentColor

@Composable
fun <T> LoadingContentBox(
    modifier: Modifier = Modifier,
    dataResult: () -> Result<T>?,
    failedText: (Throwable) -> String,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        dataResult()?.fold(
            onSuccess = { content(it) },
            onFailure = {
                FailedContent {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = failedText(it))
                        if (onRetry != null) RetryButton(onClick = onRetry)
                    }
                }
            }
        ) ?: LoadingIndicator()
    }
}


@Composable
private fun FailedContent(
    content: @Composable () -> Unit
) {
    ProvideContentColor(cpsColors.error) {
        ProvideTextStyle(
            value = TextStyle(fontWeight = FontWeight.SemiBold),
            content = content
        )
    }
}

@Composable
private fun RetryButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = cpsColors.error
        )
    ) {
        Icon(CPSIcons.Reload, null)
        Text("Retry")
    }
}