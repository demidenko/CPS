@file:OptIn(ExperimentalPermissionsApi::class)

package com.demich.cps.ui

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

sealed interface PermissionStatus {
    data object Granted: PermissionStatus
    data class NotGranted(
        val canForceRequest: Boolean,
        val permissionRequest: () -> Unit
    ): PermissionStatus
}

@Composable
fun rememberNotificationsPermissionsState(): State<PermissionStatus> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val state = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

        return remember(state) {
            computedStateOf {
                when (val it = state.status) {
                    is com.google.accompanist.permissions.PermissionStatus.Granted -> PermissionStatus.Granted
                    is com.google.accompanist.permissions.PermissionStatus.Denied ->
                        PermissionStatus.NotGranted(
                            canForceRequest = !it.shouldShowRationale,
                            permissionRequest = state::launchPermissionRequest
                        )
                }
            }
        }
    } else {
        return remember {
            computedStateOf { PermissionStatus.Granted }
        }
    }
}

private const val messageNotifications = "Some features you turned on use notifications for full functionality. Grant the permission for CPS to post notifications."