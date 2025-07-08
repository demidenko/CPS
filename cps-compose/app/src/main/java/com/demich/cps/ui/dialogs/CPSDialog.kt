package com.demich.cps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demich.cps.ui.CPSCheckBoxTitled
import com.demich.cps.ui.CPSRadioButtonTitled
import com.demich.cps.ui.theme.cpsColors


@Composable
fun CPSDialog(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
//            usePlatformDefaultWidth = false, //affects height!!!
        )
    ) {
        //TODO: remove it (includeFontPadding = false)
        ProvideTextStyle(TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true))) {
            Card(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = cpsColors.backgroundAdditional,
                modifier = Modifier
                    .imePadding()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = modifier.padding(all = 18.dp),
                    horizontalAlignment = horizontalAlignment,
                    content = content
                )
            }
        }
    }
}

@Composable
fun CPSDialog(
    modifier: Modifier = Modifier,
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    CPSDialog(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            content()
        }
    )
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

@Composable
fun CPSDeleteDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) = CPSDeleteDialog(
    title = { Text(title) },
    onDismissRequest = onDismissRequest,
    onConfirmRequest = onConfirmRequest
)


@Composable
fun CPSDeleteDialog(
    title: AnnotatedString,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) = CPSDeleteDialog(
    title = { Text(title) },
    onDismissRequest = onDismissRequest,
    onConfirmRequest = onConfirmRequest
)

@Composable
fun CPSDeleteDialog(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    CPSAskDialog(
        title = title,
        dismissButtonContent = { Text(text = "Cancel") },
        confirmButtonContent = { Text(text = "Delete", color = cpsColors.error) },
        onDismissRequest = onDismissRequest,
        onConfirmRequest = onConfirmRequest
    )
}

@Composable
fun CPSYesNoDialog(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    CPSAskDialog(
        title = title,
        dismissButtonContent = { Text(text = "No") },
        confirmButtonContent = { Text(text = "Yes") },
        onDismissRequest = onDismissRequest,
        onConfirmRequest = onConfirmRequest
    )
}

@Composable
fun CPSAskDialog(
    title: @Composable () -> Unit,
    dismissButtonContent: @Composable RowScope.() -> Unit,
    confirmButtonContent: @Composable RowScope.() -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                content = confirmButtonContent,
                onClick = {
                    onConfirmRequest()
                    onDismissRequest()
                }
            )
        },
        dismissButton = {
            TextButton(
                content = dismissButtonContent,
                onClick = onDismissRequest
            )
        },
        title = title,
        backgroundColor = cpsColors.backgroundAdditional,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun <T> CPSDialogSelect(
    title: String,
    options: Iterable<T>,
    selectedOption: T,
    optionTitle: @Composable (T) -> Unit,
    onDismissRequest: () -> Unit,
    onSelectOption: (T) -> Unit
) {
    CPSDialog(
        title = title,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, false)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                CPSRadioButtonTitled(
                    title = { optionTitle(option) },
                    selected = option == selectedOption,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onSelectOption(option)
                    onDismissRequest()
                }
            }
        }
    }
}

@Composable
fun<T: Enum<T>> CPSDialogMultiSelectEnum(
    title: String,
    options: Iterable<T>,
    selectedOptions: Collection<T>,
    optionContent: @Composable (T) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveSelected: (Set<T>) -> Unit
) {
    var currentlySelected: Set<T> by remember { mutableStateOf(selectedOptions.toSet()) }

    CPSDialog(
        title = title,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, false)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                CPSCheckBoxTitled(
                    title = { optionContent(option) },
                    checked = option in currentlySelected,
                    modifier = Modifier.fillMaxWidth()
                ) { checked ->
                    if (checked) currentlySelected += option
                    else currentlySelected -= option
                }
            }
            CPSDialogCancelAcceptButtons(
                acceptTitle = "Save",
                onCancelClick = onDismissRequest,
                onAcceptClick = {
                    onSaveSelected(currentlySelected)
                    onDismissRequest()
                }
            )
        }
    }
}