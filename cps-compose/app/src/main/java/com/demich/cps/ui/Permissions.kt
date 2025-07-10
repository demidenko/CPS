@file:OptIn(ExperimentalPermissionsApi::class)

package com.demich.cps.ui

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
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
        PermissionPanel(
            modifier = modifier,
            message = "Some features you turned on use notifications for full functionality. Please grant the permission for CPS to post notifications.",
            onClick = { state.launchPermissionRequest() },
        )
    }

    DisposableEffect(state) {
        if (!state.status.isGranted && !state.status.shouldShowRationale) {
            state.launchPermissionRequest()
        }
        onDispose {  }
    }
}

@Composable
private fun PermissionPanel(
    modifier: Modifier = Modifier,
    message: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(color = cpsColors.backgroundAdditional, shape = shape)
            .border(color = cpsColors.error, width = 1.dp, shape = shape)
            .padding(all = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AttentionIcon(
                dangerType = DangerType.DANGER,
                size = 19.sp
            )
            Text(
                text = "Permissions",
                fontWeight = FontWeight.SemiBold,
                color = cpsColors.error
            )
        }

        Text(
            text = message,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = onClick,
            content = { Text(text = "Allow permission") },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = cpsColors.content
            )
        )
    }
}

@Preview
@Composable
private fun PreviewPermissionPanel() {
    CPSTheme {
        PermissionPanel(
            message = "Please grand permission for full functionality",
            onClick = { }
        )
    }
}