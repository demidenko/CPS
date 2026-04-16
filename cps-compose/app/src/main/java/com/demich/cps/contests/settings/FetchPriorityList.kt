package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.database.toContestPlatform
import com.demich.cps.contests.fetching.ContestsFetchSource
import com.demich.cps.platforms.Platform
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.datastore_itemized.edit
import com.demich.kotlin_stdlib_boost.swap
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun FetchPriorityListDialog(
    platform: Platform,
    availableOptions: Set<ContestsFetchSource>,
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    val settings = remember { context.settingsContests }
    val priorityList by collectAsState {
        val platform = platform.toContestPlatform()
        settings.fetchPriorityLists.asFlow().map { it.getValue(platform) }
    }

    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        Text(text = "$platform fetch priority list = ")
        FetchPriorityList(
            modifier = Modifier.padding(vertical = 4.dp),
            priorityList = priorityList,
            availableOptions = availableOptions,
            onListChange = { newList ->
                scope.launch {
                    settings.fetchPriorityLists.edit {
                        this[platform.toContestPlatform()] = newList
                    }
                }
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = CPSIcons.Info,
                contentDescription = null,
                modifier = Modifier.padding(all = 8.dp),
                tint = cpsColors.contentAdditional
            )
            Text(
                text = "Order of execution of fetching contests until success.",
                color = cpsColors.contentAdditional,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun FetchPriorityList(
    modifier: Modifier = Modifier,
    priorityList: List<ContestsFetchSource>,
    availableOptions: Set<ContestsFetchSource>,
    onListChange: (List<ContestsFetchSource>) -> Unit
) {
    require(priorityList.isNotEmpty())
    Column(modifier = modifier) {
        priorityList.forEachIndexed { index, source ->
            PriorityListItem(
                fetchSource = source,
                index = index + 1,
                deleteEnabled = priorityList.size > 1,
                onDeleteRequest = {
                    val newList = priorityList.toMutableList()
                    newList.removeAt(index)
                    onListChange(newList)
                },
                availableOptions = availableOptions,
                onOptionSelected = { newSource ->
                    check(newSource != source)
                    val newList = priorityList.toMutableList()
                    val i = newList.indexOf(newSource)
                    val j = newList.indexOf(source)
                    if (i == -1) newList[j] = newSource
                    else newList.swap(i, j)
                    onListChange(newList)
                }
            )
        }
        if (!priorityList.containsAll(availableOptions)) {
            PriorityListItemAdd(
                availableOptions = availableOptions - priorityList,
                onOptionSelected = { fetchSource ->
                    check(fetchSource !in priorityList)
                    onListChange(priorityList + fetchSource)
                }
            )
        }
    }
}

@Composable
private fun PriorityListItem(
    modifier: Modifier = Modifier,
    fetchSource: ContestsFetchSource,
    index: Int,
    deleteEnabled: Boolean,
    onDeleteRequest: () -> Unit,
    availableOptions: Set<ContestsFetchSource>,
    onOptionSelected: (ContestsFetchSource) -> Unit
) {
    PriorityListItem(
        modifier = modifier,
        text = "$index. $fetchSource",
        options = availableOptions - fetchSource,
        onOptionSelected = onOptionSelected,
        onDeleteRequest = onDeleteRequest.takeIf { deleteEnabled }
    )
}

@Composable
private fun PriorityListItemAdd(
    modifier: Modifier = Modifier,
    availableOptions: Set<ContestsFetchSource>,
    onOptionSelected: (ContestsFetchSource) -> Unit
) {
    PriorityListItem(
        modifier = modifier,
        text = "+ add",
        options = availableOptions,
        onOptionSelected = onOptionSelected
    )
}

@Composable
private fun PriorityListItem(
    modifier: Modifier = Modifier,
    text: String,
    options: Set<ContestsFetchSource>,
    onOptionSelected: (ContestsFetchSource) -> Unit,
    onDeleteRequest: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    ContentWithCPSDropdownMenu(
        modifier = modifier
            .clickable { showMenu = true }
            .padding(all = 6.dp),
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        content = { Text(text = text) }
    ) {
        FetchSourcesMenu(
            options = options,
            onDeleteRequest = onDeleteRequest,
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun CPSDropdownMenuScope.FetchSourcesMenu(
    options: Set<ContestsFetchSource>,
    onDeleteRequest: (() -> Unit)?,
    onOptionSelected: (ContestsFetchSource) -> Unit
) {
    options.forEach { option ->
        CPSDropdownMenuItem(
            title = option.name,
            icon = CPSIcons.Insert,
            onClick = { onOptionSelected(option) }
        )
    }
    if (onDeleteRequest != null) {
        CPSDropdownMenuItem(
            title = "delete",
            icon = CPSIcons.Delete,
            onClick = onDeleteRequest
        )
    }
}
