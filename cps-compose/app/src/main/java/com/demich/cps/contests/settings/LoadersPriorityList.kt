package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.Contest
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun LoadersPriorityListDialog(
    platform: Contest.Platform,
    availableOptions: Set<ContestsLoaders>,
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    val settings = remember { context.settingsContests }
    val priorityList by rememberCollect {
        settings.contestsLoadersPriorityLists.flow.map { it.getValue(platform) }
    }

    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        Text(text = "$platform loading priority list = ")
        LoadersPriorityList(
            modifier = Modifier.padding(vertical = 4.dp),
            priorityList = priorityList,
            availableOptions = availableOptions,
            onListChange = { newList ->
                scope.launch {
                    settings.contestsLoadersPriorityLists.edit {
                        this[platform] = newList
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
                text = "Order in which contest api-loaders are executed until success.",
                color = cpsColors.contentAdditional,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LoadersPriorityList(
    modifier: Modifier = Modifier,
    priorityList: List<ContestsLoaders>,
    availableOptions: Set<ContestsLoaders>,
    onListChange: (List<ContestsLoaders>) -> Unit
) {
    require(priorityList.isNotEmpty())
    Column(modifier = modifier) {
        priorityList.forEachIndexed { index, loaderType ->
            PriorityListItemLoader(
                loaderType = loaderType,
                index = index + 1,
                deleteEnabled = priorityList.size > 1,
                onDeleteRequest = {
                    val newList = priorityList.toMutableList()
                    newList.removeAt(index)
                    onListChange(newList)
                },
                availableOptions = availableOptions,
                onOptionSelected = { newType ->
                    require(newType != loaderType)
                    val newList = priorityList.toMutableList()
                    newList.remove(newType)
                    newList[newList.indexOf(loaderType)] = newType
                    onListChange(newList)
                }
            )
        }
        if (!priorityList.containsAll(availableOptions)) {
            PriorityListItemAdd(
                availableOptions = availableOptions - priorityList,
                onOptionSelected = { loaderType ->
                    require(loaderType !in priorityList)
                    onListChange(priorityList + loaderType)
                }
            )
        }
    }
}

@Composable
private fun PriorityListItemLoader(
    modifier: Modifier = Modifier,
    loaderType: ContestsLoaders,
    index: Int,
    deleteEnabled: Boolean,
    onDeleteRequest: () -> Unit,
    availableOptions: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    PriorityListItem(
        modifier = modifier,
        text = "$index. $loaderType",
        options = availableOptions - loaderType,
        onOptionSelected = onOptionSelected,
        onDeleteRequest = onDeleteRequest.takeIf { deleteEnabled }
    )
}

@Composable
private fun PriorityListItemAdd(
    modifier: Modifier = Modifier,
    availableOptions: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit
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
    options: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit,
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
        LoadersMenu(
            options = options,
            onDeleteRequest = onDeleteRequest,
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun CPSDropdownMenuScope.LoadersMenu(
    options: Set<ContestsLoaders>,
    onDeleteRequest: (() -> Unit)?,
    onOptionSelected: (ContestsLoaders) -> Unit
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
