package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu

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
            PriorityListItem(
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
                },
                modifier = Modifier.padding(all = 2.dp)
            )
        }
        if (!priorityList.containsAll(availableOptions)) {
            PriorityListItemAdd(
                availableOptions = availableOptions - priorityList,
                onOptionSelected = { loaderType ->
                    require(loaderType !in priorityList)
                    onListChange(priorityList + loaderType)
                },
                modifier = Modifier.padding(all = 2.dp)
            )
        }
    }
}

@Composable
private fun PriorityListItem(
    modifier: Modifier = Modifier,
    loaderType: ContestsLoaders,
    index: Int,
    deleteEnabled: Boolean,
    onDeleteRequest: () -> Unit,
    availableOptions: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ContentWithCPSDropdownMenu(
        modifier = modifier.clickable { showMenu = true },
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        content = {
            Text(text = "$index. $loaderType")
        }
    ) {
        LoadersMenu(
            options = availableOptions - loaderType,
            deleteOption = deleteEnabled,
            onDeleteRequest = onDeleteRequest,
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun PriorityListItemAdd(
    modifier: Modifier = Modifier,
    availableOptions: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ContentWithCPSDropdownMenu(
        modifier = modifier.clickable { showMenu = true },
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        content = { Text(text = "+ add") }
    ) {
        LoadersMenu(
            options = availableOptions,
            deleteOption = false,
            onDeleteRequest = {},
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun CPSDropdownMenuScope.LoadersMenu(
    options: Set<ContestsLoaders>,
    deleteOption: Boolean,
    onDeleteRequest: () -> Unit,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    options.forEach { option ->
        CPSDropdownMenuItem(
            title = option.name,
            icon = CPSIcons.Insert,
            onClick = { onOptionSelected(option) }
        )
    }
    if (deleteOption) {
        CPSDropdownMenuItem(
            title = "delete",
            icon = CPSIcons.Delete,
            onClick = onDeleteRequest
        )
    }
}
