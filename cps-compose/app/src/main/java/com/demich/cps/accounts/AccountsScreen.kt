package com.demich.cps.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.demich.cps.R
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors


@Composable
fun AccountsScreen(navController: NavController) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(cpsColors.background)
    ) {
        MonospacedText(
            text = stringResource(id = R.string.accounts_welcome),
            color = cpsColors.textColorAdditional,
            fontSize = 18.sp,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
fun AccountsBottomBar() {
    CPSIconButton(icon = Icons.Outlined.AddBox) {
        //TODO Open Add new account
    }
    CPSIconButton(icon = Icons.Default.Refresh) {
        //TODO Reload Accounts
    }
}