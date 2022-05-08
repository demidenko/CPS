package com.demich.cps.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demich.cps.ui.theme.cpsColors

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CPSDialog(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = cpsColors.background,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = modifier.padding(all = 18.dp),
                horizontalAlignment = horizontalAlignment,
                content = content
            )
        }
    }
}

@Composable
fun CPSDialogCancelAcceptButtons(
    modifier: Modifier = Modifier,
    cancelTitle: String = "Cancel",
    acceptTitle: String,
    acceptEnabled: Boolean = true,
    onCancelClick: () -> Unit,
    onAcceptClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier.fillMaxWidth()
    ) {
        TextButton(onClick = onCancelClick) { Text(cancelTitle) }
        TextButton(
            onClick = onAcceptClick,
            enabled = acceptEnabled,
            content = { Text(acceptTitle) }
        )
    }
}