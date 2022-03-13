package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDialog
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.showToast
import kotlinx.coroutines.delay
import kotlin.reflect.KFunction1

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun<U: UserInfo> DialogAccountChooser(
    manager: AccountManager<U>,
    onDismissRequest: () -> Unit,
    onResult: (U) -> Unit
) {
    val context = context
    val charValidator: KFunction1<Char, Boolean> =
        if (manager is AccountSuggestionsProvider) manager::isValidForSearch
        else manager::isValidForUserId

    CPSDialog(onDismissRequest = onDismissRequest) {
        val iconSize = 32.dp
        val inputTextSize = 18.sp
        val resultTextSize = 14.sp

        var userId by remember { mutableStateOf("") }
        var userInfo by remember { mutableStateOf(manager.emptyInfo()) }
        var loading by remember { mutableStateOf(false) }
        var showLoading by remember { mutableStateOf(false) }

        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        val done = {
            if (userInfo.status != STATUS.NOT_FOUND && !loading) {
                if (userId.all(manager::isValidForUserId)) {
                    onResult(userInfo)
                    onDismissRequest()
                } else {
                    context.showToast("${manager.userIdTitle} contains unacceptable symbols")
                }
            }
        }

        MonospacedText(
            text = "getUser(${manager.managerName}):",
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = userId,
            modifier = Modifier.focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(fontSize = inputTextSize),
            onValueChange = { str ->
                if (!str.all(charValidator)) return@TextField
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
                    text = makeUserInfoSpan(userInfo, manager),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = resultTextSize
                )
            },
            trailingIcon = {
                if (showLoading || userInfo.status != STATUS.NOT_FOUND)
                    IconButton(
                        onClick = done,
                        enabled = !loading
                    ) {
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
                                    .border(
                                        border = ButtonDefaults.outlinedBorder,
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { done() }
            ),
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

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            delay(300) //TODO fix this shit
            keyboardController?.show()
        }
    }
}

@Composable
fun<U: UserInfo> makeUserInfoSpan(userInfo: U, manager: AccountManager<U>): AnnotatedString {
    if (userInfo.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        withStyle(SpanStyle(color = cpsColors.textColor)) {
            when (userInfo.status) {
                STATUS.OK -> append(manager.makeOKInfoSpan(userInfo))
                STATUS.NOT_FOUND -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("User not found")
                    }
                }
                STATUS.FAILED -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("Load failed")
                    }
                }
            }
        }
    }
}