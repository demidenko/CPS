package com.demich.cps.contests.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.Contest
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.launch

@Composable
fun ClistAdditionalPlatformsSettingsItem(
    item: CPSDataStoreItem<List<ClistResource>>
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = "Clist additional platforms"
    ) { resources ->
        Text(
            text = resources.joinToString { it.name },
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (showDialog) {
        ClistAdditionalResourcesDialog(
            item = item,
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun ClistAdditionalResourcesDialog(
    item: CPSDataStoreItem<List<ClistResource>>,
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    var loadingStatus by remember { mutableStateOf(LoadingStatus.LOADING) }
    var unselectedItems: List<ClistResource> by remember { mutableStateOf(emptyList()) }

    val selectedItems by rememberCollect { item.flow }

    LaunchedEffect(Unit) {
        loadingStatus = LoadingStatus.LOADING
        kotlin.runCatching {
            CListApi.getResources(apiAccess = context.settingsContests.clistApiAccess())
        }.onFailure {
            loadingStatus = LoadingStatus.FAILED
        }.onSuccess { resources ->
            val alreadySupported =
                Contest.platformsExceptUnknown.map { CListUtils.getClistApiResourceId(it) }
            unselectedItems = resources
                .filter { it.id !in alreadySupported }
                .sortedBy { it.name }
            loadingStatus = LoadingStatus.PENDING
        }
    }

    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        Text(text = "selected:")
        SelectedPlatforms(
            resources = selectedItems,
            modifier = Modifier
                .padding(bottom = 3.dp)
                .heightIn(max = 200.dp)
        ) { resource ->
            scope.launch { item.remove(resource) }
        }

        Text(text = "available:")
        UnselectedPlatforms(
            loadingStatus = loadingStatus,
            resources = unselectedItems - selectedItems,
            modifier = Modifier
                .heightIn(max = 200.dp),
            onClick = { resource ->
                scope.launch { item(newValue = listOf(resource) + item()) }
            }
        )


    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectedPlatforms(
    modifier: Modifier = Modifier,
    resources: List<ClistResource>,
    onClick: (ClistResource) -> Unit
) {
    LazyColumnWithScrollBar(modifier = modifier) {
        items(items = resources, key = { it.id }) { resource ->
            Text(
                text = resource.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClick(resource)
                    }
                    .padding(all = 2.dp)
                    .animateItemPlacement()
            )
            Divider()
        }
    }
}

@Composable
private fun UnselectedPlatforms(
    modifier: Modifier = Modifier,
    loadingStatus: LoadingStatus,
    resources: List<ClistResource>,
    onClick: (ClistResource) -> Unit
) {
    LoadingContentBox(
        loadingStatus = loadingStatus,
        failedText = "Failed to load resources",
        modifier = modifier
    ) {
        var searchFilter by remember { mutableStateOf("") }
        Column {
            OutlinedTextField(
                value = searchFilter,
                onValueChange = { searchFilter = it },
                leadingIcon = {
                    Icon(
                        imageVector = CPSIcons.Search,
                        contentDescription = null,
                        modifier = modifier.size(32.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumnWithScrollBar {
                items(
                    items = resources.filter { it.name.contains(searchFilter, ignoreCase = true) },
                    key = { it.id }
                ) { resource ->
                    Text(
                        text = resource.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClick(resource)
                            }
                            .padding(all = 2.dp)
                    )
                    Divider()
                }
            }
        }
    }
}