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
            LoadingStatus.LOADING -> CircularProgressIndicator(color = cpsColors.content, strokeWidth = 3.dp)
            LoadingStatus.FAILED -> {
                Text(
                    text = failedText,
                    color = cpsColors.errorColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}