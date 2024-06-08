@file:OptIn(ExperimentalPermissionsApi::class)

package com.demich.cps.ui

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale


@Composable
fun NotificationsPermissionPanel(
    permissionRequired: Boolean,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (permissionRequired) {
            NotificationsPermissionPanel(modifier = modifier)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NotificationsPermissionPanel(modifier: Modifier) {
    val state = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    if (!state.status.isGranted) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(color = cpsColors.error)
                .padding(all = 8.dp)
        ) {
            Text(
                text = "Some features you turned on use notifications for full functionality. Please grant the permission for CPS to post notifications.",
                fontWeight = FontWeight.SemiBold,
                color = contentColorFor(cpsColors.error)
            )
            OutlinedButton(
                onClick = { state.launchPermissionRequest() },
                content = { Text("Request permission") },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = cpsColors.backgroundAdditional
                )
            )
        }
    }

    DisposableEffect(state) {
        if (!state.status.isGranted && !state.status.shouldShowRationale) {
            state.launchPermissionRequest()
        }
        onDispose {  }
    }
}