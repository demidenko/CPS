package com.demich.cps.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.LazyColumnWithScrollBar
import kotlinx.coroutines.launch

@Composable
fun NewsScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val n = 270.toInt()
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        var inputIndex by remember { mutableStateOf("") }
        val isValid by remember(inputIndex) {
            derivedStateOf {
                inputIndex.toIntOrNull()?.let { num ->
                    num in 0 until n
                } ?: false
            }
        }
        TextField(
            value = inputIndex,
            onValueChange = { inputIndex = it },
            label = { Text("jump to") },
            trailingIcon = {
                CPSIconButton(
                    icon = Icons.Default.ArrowRightAlt,
                    enabledState = isValid,
                    onState = isValid
                ) {
                    scope.launch {
                        listState.scrollToItem(inputIndex.toInt())
                    }
                }
            },
            isError = !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        LazyColumnWithScrollBar(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count = n, key = { it }) { index ->
                repeat(index.countOneBits()) {
                    Text("$index[$it] = ${index.toString(2)}")
                }
                Divider()
            }
        }

    }


}


@Composable
fun NewsSettingsScreen() {

}

@Composable
fun NewsBottomBar() {
    CPSIconButton(icon = Icons.Default.Refresh) {

    }
}

