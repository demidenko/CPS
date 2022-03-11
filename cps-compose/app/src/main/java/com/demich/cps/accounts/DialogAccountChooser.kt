package com.demich.cps.accounts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var userId by remember { mutableStateOf("") }
    var userInfo by remember { mutableStateOf(manager.emptyInfo()) }
    var loading by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    CPSDialog(onDismissRequest = onDismissRequest) {
        MonospacedText(
            text = "getUser(${manager.managerName}):",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = userId,
            singleLine = true,
            onValueChange = { str ->
                if (!str.all(manager::isValidForSearch)) return@TextField
                userId = str
                userInfo = manager.emptyInfo()
                loading = userId.isNotBlank()
            }
        )
        if (loading) {
            if (showLoading) CircularProgressIndicator(color = cpsColors.textColor)
            LaunchedEffect(userId) {
                delay(300)
                showLoading = true
                userInfo = manager.loadInfo(userId, 1)
                showLoading = false
                loading = false
            }
        } else {
            Text(
                text = userInfo.let {
                    if (it.isEmpty()) "" else it.makeInfoString()
                },
                fontSize = 18.sp
            )
        }

    }
}
