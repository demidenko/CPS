package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDialog
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import kotlinx.coroutines.delay

@Composable
fun<U: UserInfo> DialogAccountChooser(
    manager: AccountManager<U>,
    onDismissRequest: () -> Unit,
    onResult: (U) -> Unit
) {
    CPSDialog(onDismissRequest = onDismissRequest) {
        var userId by remember { mutableStateOf("") }
        var userInfo by remember { mutableStateOf(manager.emptyInfo()) }
        var loading by remember { mutableStateOf(false) }
        var showLoading by remember { mutableStateOf(false) }
        MonospacedText(
            text = "getUser(${manager.managerName}):",
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = userId,
            singleLine = true,
            textStyle = TextStyle(fontSize = 18.sp),
            onValueChange = { str ->
                if (!str.all(manager::isValidForSearch)) return@TextField
                userId = str
                userInfo = manager.emptyInfo()
                loading = userId.isNotBlank()
            },
            placeholder = {
                var label = manager.userIdTitle
                if (manager is AccountSuggestionsProvider) label += " or search query"
                Text(text = label, color = cpsColors.textColorAdditional)
            },
            label = {
                Text(
                    text = infoString(userInfo, manager),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
            },
            trailingIcon = {
                if (showLoading || userInfo.status != STATUS.NOT_FOUND)
                    IconButton(
                        onClick = { onResult(userInfo) },
                        enabled = !loading
                    ) {
                        val iconSize = 32.dp
                        if (showLoading) {
                            CircularProgressIndicator(
                                color = cpsColors.textColor,
                                modifier = Modifier.size(iconSize),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                tint = cpsColors.success,
                                modifier = Modifier
                                    .size(iconSize)
                                    .border(border = ButtonDefaults.outlinedBorder, shape = MaterialTheme.shapes.small)
                            )
                        }
                    }
            }
        )
        if (loading) {
            LaunchedEffect(userId) {
                delay(300)
                showLoading = true
                userInfo = manager.loadInfo(userId, 1)
                showLoading = false
                loading = false
            }
        } else {
            showLoading = false
        }
    }
}

@Composable
private fun<U: UserInfo> infoString(userInfo: U, manager: AccountManager<U>): AnnotatedString {
    return buildAnnotatedString {
        if (userInfo.isEmpty()) return@buildAnnotatedString
        when (userInfo.status) {
            STATUS.OK -> {
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = manager.getColor(userInfo)?.let { Color(it) } ?: cpsColors.textColor
                )) {
                    append(userInfo.makeInfoString())
                }
            }
            STATUS.NOT_FOUND -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = cpsColors.textColor)) {
                    append("User not found")
                }
            }
            STATUS.FAILED -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = cpsColors.textColor)) {
                    append("Load failed")
                }
            }
        }
    }
}