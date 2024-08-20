package com.demich.cps.ui.filter

import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.rememberFocusOnCreationRequester


@Composable
fun FilterTextField(
    filterState: FilterState,
    modifier: Modifier = Modifier
) {
    if (filterState.enabled) {
        val focusRequester = rememberFocusOnCreationRequester()
        OutlinedTextField(
            modifier = modifier.focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            value = filterState.filter,
            onValueChange = {
                filterState.filter = it
            },
            label = { Text("filter") },
            leadingIcon = {
                Icon(
                    imageVector = CPSIcons.Search,
                    tint = cpsColors.content,
                    contentDescription = null
                )
            },
            trailingIcon = {
                CPSIconButton(icon = CPSIcons.Close) {
                    filterState.enabled = false
                }
            }
        )
    }
}

