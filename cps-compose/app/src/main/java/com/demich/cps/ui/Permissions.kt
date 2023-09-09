@file:OptIn(ExperimentalPermissionsApi::class)

package com.demich.cps.ui

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun NotificationPermissionsPanel(
    permissionsRequired: Boolean,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (permissionsRequired) {
            NotificationPermissionsPanel(modifier = modifier)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NotificationPermissionsPanel(modifier: Modifier) {
    val state = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    if (!state.status.isGranted) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(color = cpsColors.backgroundAdditional)
                .padding(all = 5.dp)
        ) {
            Text(
                text = "The features you turned on use notifications for full functionality. Please grant the permission for CPS to post notifications.",
                fontWeight = FontWeight.SemiBold,
                color = cpsColors.error
            )
            Button(
                onClick = { state.launchPermissionRequest() },
                content = { Text("Request permission") }
            )
        }
    }
    LaunchedEffect(Unit) {
        if (!state.status.isGranted && !state.status.shouldShowRationale) {
            state.launchPermissionRequest()
        }
    }
}