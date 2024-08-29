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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.ClistResource
import com.demich.cps.platforms.api.niceMessage
import com.demich.cps.platforms.utils.ClistUtils
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ListTitle
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.datastore_itemized.edit
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.coroutines.launch

@Composable
internal fun ClistAdditionalResourcesDialog(
    onDismissRequest: () -> Unit
) {
    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        DialogContent()
    }
}

@Composable
private fun ColumnScope.DialogContent() {
    val context = context
    val scope = rememberCoroutineScope()

    val dataLoader = remember(scope) { BackgroundDataLoader<List<ClistResource>>(scope = scope) }
    val resourcesResult by dataLoader.flowOfResult().collectAsState()

    LaunchedEffect(dataLoader) {
        dataLoader.execute(id = Unit) {
            val alreadySupported = Contest.platformsExceptUnknown.mapToSet(ClistUtils::getClistApiResourceId)
            ClistApi.getResources(apiAccess = context.settingsContests.clistApiAccess())
                .filter { it.id !in alreadySupported }
                .sortedBy { it.name }
        }
    }

    val item = remember { context.settingsContests.clistAdditionalResources }
    val selected by collectItemAsState { item }

    var searchFilter by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.weight(1f, fill = false),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ListTitle(text = "selected:")
        ClistResourcesList(
            resources = { filter(selected, searchFilter) },
            modifier = Modifier
                .padding(bottom = 3.dp)
                .heightIn(max = 200.dp),
            onItemClick = { resource ->
                scope.launch { item.edit { remove(resource) } }
            }
        )

        ListTitle(text = "available:")
        LoadingContentBox(
            dataResult = { resourcesResult?.map { it - selected.toSet() } },
            failedText = { it.niceMessage ?: "Failed to load resources" },
            modifier = Modifier
                .padding(bottom = 5.dp)
                .fillMaxWidth()
        ) { resources ->
            ClistResourcesList(
                resources = { filter(resources, searchFilter) },
                onItemClick = { resource ->
                    scope.launch { item.edit { add(index = 0, element = resource) } }
                }
            )
        }
    }

    OutlinedTextField(
        value = searchFilter,
        onValueChange = { searchFilter = it },
        leadingIcon = {
            Icon(
                imageVector = CPSIcons.Search,
                contentDescription = null,
                tint = cpsColors.content
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun filter(resources: List<ClistResource>, searchFilter: String) =
    resources.filter { it.name.contains(searchFilter, ignoreCase = true) }


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClistResourcesList(
    resources: () -> List<ClistResource>,
    modifier: Modifier = Modifier,
    onItemClick: (ClistResource) -> Unit
) {
    LazyColumnWithScrollBar(modifier = modifier) {
        items(items = resources(), key = { it.id }) { resource ->
            Text(
                text = resource.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(resource) }
                    .padding(all = 2.dp)
                    .animateItemPlacement()
            )
            Divider(modifier = Modifier.animateItemPlacement())
        }
    }
}