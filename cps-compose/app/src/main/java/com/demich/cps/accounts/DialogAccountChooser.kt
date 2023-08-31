package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.*
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberFocusOnCreationRequester
import com.demich.cps.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val suggestionsMinLength = 3

@Composable
fun<U: UserInfo> DialogAccountChooser(
    manager: AccountManager<U>,
    initialUserInfo: U?,
    onDismissRequest: () -> Unit,
    onResult: (U) -> Unit
) {
    CPSDialog(onDismissRequest = onDismissRequest) {
        AccountChooserHeader(
            text = "getUser(${manager.type.name}):",
            color = cpsColors.contentAdditional
        ) {
            Icon(imageVector = CPSIcons.Account, contentDescription = null, tint = it)
        }

        DialogContent(
            manager = manager,
            initialUserInfo = initialUserInfo,
            onDismissRequest = onDismissRequest,
            onResult = onResult,
            charValidator = when (manager) {
                is UserSuggestionsProvider -> manager::isValidForSearch
                else -> manager::isValidForUserId
            }
        )
    }
}

@Composable
private fun<U: UserInfo> DialogContent(
    manager: AccountManager<U>,
    initialUserInfo: U?,
    onDismissRequest: () -> Unit,
    onResult: (U) -> Unit,
    charValidator: (Char) -> Boolean
) {
    val context = context

    var textFieldValue by remember {
        mutableStateOf((initialUserInfo?.userId ?: "").toTextFieldValue())
    }
    val userId by rememberUpdatedState(newValue = textFieldValue.text)

    var userInfo by remember { mutableStateOf(initialUserInfo) }
    var loadingInProgress by remember { mutableStateOf(false) }

    var suggestionsResult by remember { mutableStateOf(Result.success(emptyList<UserSuggestion>())) }
    var loadingSuggestionsInProgress by remember { mutableStateOf(false) }
    var blockSuggestionsReload by remember { mutableStateOf(false) }

    val focusRequester = rememberFocusOnCreationRequester()
    var firstLaunch by remember { mutableStateOf(true) }

    UserIdTextField(
        manager = manager,
        userInfo = userInfo,
        textFieldValue = textFieldValue,
        onValueChange = {
            if (it.text.all(charValidator)) textFieldValue = it
        },
        loadingInProgress = loadingInProgress,
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(),
        inputTextSize = 18.sp,
        resultTextSize = 14.sp
    ) {
        if (userId.all(manager::isValidForUserId)) {
            onResult(it)
            onDismissRequest()
        } else {
            context.showToast("${manager.userIdTitle} contains unacceptable symbols")
        }
    }

    if (manager is UserSuggestionsProvider) {
        SuggestionsList(
            suggestions = suggestionsResult.getOrDefault(emptyList()),
            isLoading = loadingSuggestionsInProgress,
            isError = suggestionsResult.isFailure,
            modifier = Modifier.fillMaxWidth(),
            onClick = { suggestion ->
                blockSuggestionsReload = true
                textFieldValue = suggestion.userId.toTextFieldValue()
            }
        )
    }

    LaunchedEffect(userId) {
        if (firstLaunch) {
            firstLaunch = false
            return@LaunchedEffect
        }
        if (userId.length < suggestionsMinLength) {
            suggestionsResult = Result.success(emptyList())
            loadingSuggestionsInProgress = false
            blockSuggestionsReload = false
        }
        userInfo = null
        if (userId.isBlank()) {
            loadingInProgress = false
            return@LaunchedEffect
        }
        delay(300)
        launch(Dispatchers.IO) {
            loadingInProgress = true
            userInfo = manager.loadInfo(userId)
            loadingInProgress = false
        }
        if (manager is UserSuggestionsProvider && userId.length >= suggestionsMinLength) {
            if (!blockSuggestionsReload) {
                loadingSuggestionsInProgress = true
                launch(Dispatchers.IO) {
                    val result = manager.runCatching { getSuggestions(userId) }
                    loadingSuggestionsInProgress = false
                    if (isActive) { //Because of "StandaloneCoroutine was cancelled" exception during cancelling LaunchedEffect
                        suggestionsResult = result
                    }
                }
            } else {
                blockSuggestionsReload = false
            }
        }
    }
}

@Composable
private fun<U: UserInfo> UserIdTextField(
    manager: AccountManager<U>,
    userInfo: U?,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    loadingInProgress: Boolean,
    modifier: Modifier,
    inputTextSize: TextUnit,
    resultTextSize: TextUnit,
    onDoneRequest: (U) -> Unit
) {
    val onDoneRequestWithCheck = {
        if (userInfo != null && userInfo.status != STATUS.NOT_FOUND && !loadingInProgress) {
            onDoneRequest(userInfo)
        }
    }
    TextField(
        value = textFieldValue,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(fontSize = inputTextSize, fontFamily = FontFamily.Monospace),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = cpsColors.background
        ),
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = buildString {
                    append(manager.userIdTitle)
                    if (manager is UserSuggestionsProvider) append(" or search query")
                },
                color = cpsColors.contentAdditional
            )
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
            TextFieldMainIcon(
                loadingInProgress = loadingInProgress,
                userInfoStatus = userInfo?.status ?: STATUS.NOT_FOUND,
                iconSize = 32.dp,
                onDoneClick = onDoneRequestWithCheck
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDoneRequestWithCheck() }),
    )
}

@Composable
private fun TextFieldMainIcon(
    loadingInProgress: Boolean,
    userInfoStatus: STATUS,
    iconSize: Dp,
    onDoneClick: () -> Unit
) {
    if (loadingInProgress || userInfoStatus != STATUS.NOT_FOUND) {
        IconButton(
            onClick = onDoneClick,
            enabled = !loadingInProgress
        ) {
            if (loadingInProgress) {
                CircularProgressIndicator(
                    color = cpsColors.content,
                    modifier = Modifier.size(iconSize),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = CPSIcons.Done,
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
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<UserSuggestion>,
    isLoading: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onClick: (UserSuggestion) -> Unit
) {
    if (suggestions.isNotEmpty() || isLoading || isError) {
        Column(modifier = modifier) {
            AccountChooserHeader(
                text = if (isError) "suggestions load failed" else "suggestions:",
                color = if (isError) cpsColors.error else cpsColors.contentAdditional
            ) { color ->
                if (isLoading) {
                    CircularProgressIndicator(
                        color = color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = CPSIcons.Search,
                        tint = color,
                        contentDescription = null
                    )
                }
            }
            LazyColumnWithScrollBar {
                items(suggestions, key = { it.userId }) {
                    SuggestionItem(
                        suggestion = it,
                        modifier = Modifier
                            .clickable { onClick(it) }
                            .padding(3.dp)
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: UserSuggestion,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Text(
            text = suggestion.title,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (suggestion.info.isNotBlank()) {
            Text(
                text = suggestion.info,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
    }
}

@Composable
private fun AccountChooserHeader(
    text: String,
    color: Color,
    icon: @Composable (Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier
            .padding(3.dp)
            .size(18.dp)) {
            icon(color)
        }
        MonospacedText(
            text = text,
            fontSize = 14.sp,
            color = color
        )
    }
}

@Composable
private fun<U: UserInfo> makeUserInfoSpan(userInfo: U?, manager: AccountManager<U>): AnnotatedString {
    if (userInfo == null) return AnnotatedString("")
    return buildAnnotatedString {
        withStyle(SpanStyle(color = cpsColors.content)) {
            when (userInfo.status) {
                STATUS.OK -> append(manager.makeOKInfoSpan(userInfo))
                STATUS.NOT_FOUND -> append(text = "User not found", fontStyle = FontStyle.Italic)
                STATUS.FAILED -> append(text = "Loading failed", fontStyle = FontStyle.Italic)
            }
        }
    }
}


private fun String.toTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))
