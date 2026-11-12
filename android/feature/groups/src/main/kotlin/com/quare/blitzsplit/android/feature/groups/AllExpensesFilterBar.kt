package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.SearchBarField
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiState
import com.quare.blitzsplit.feature.groups.presentation.model.ExpenseSort
import com.quare.blitzsplit.feature.groups.presentation.model.PayerFilter
import com.quare.blitzsplit.feature.groups.presentation.model.ShareFilter

/*
 * Why: over "All expenses"' bills (Pierre's call, beta feedback): the search, which finds bills by their description as
 * the home's search does, then a row of chips that scrolls sideways: the order, then Group, Category, Your share and
 * Who paid, each a menu, a chip picked while its filter narrows the list, and "Clear filters" once one does.
 */
@Composable
internal fun AllExpensesFilterBar(
    state: AllExpensesUiState,
    onEvent: (AllExpensesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = state.filters
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchBarField(
            query = filters.query,
            placeholder = stringResource(R.string.groups_all_expenses_search),
            clearLabel = stringResource(R.string.groups_search_clear),
            onQueryChange = { onEvent(AllExpensesUiEvent.OnQueryChanged(it)) },
            height = 52.dp,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortChip(
                sort = filters.sort,
                onPick = { onEvent(AllExpensesUiEvent.OnSortPicked(it)) },
            )
            GroupChip(
                state = state,
                onEvent = onEvent,
            )
            CategoryChip(
                state = state,
                onEvent = onEvent,
            )
            MenuChip(
                label = if (filters.share == ShareFilter.ALL) {
                    stringResource(R.string.groups_all_expenses_share)
                } else {
                    shareLabel(filters.share)
                },
                isSelected = filters.share != ShareFilter.ALL,
            ) { close ->
                ShareFilter.entries.forEach { share ->
                    CheckedMenuItem(
                        text = shareLabel(share),
                        isChecked = filters.share == share,
                    ) {
                        close()
                        onEvent(AllExpensesUiEvent.OnShareFilterPicked(share))
                    }
                }
            }
            MenuChip(
                label = if (filters.payer == PayerFilter.ALL) {
                    stringResource(R.string.groups_all_expenses_payer)
                } else {
                    payerLabel(filters.payer)
                },
                isSelected = filters.payer != PayerFilter.ALL,
            ) { close ->
                PayerFilter.entries.forEach { payer ->
                    CheckedMenuItem(
                        text = payerLabel(payer),
                        isChecked = filters.payer == payer,
                    ) {
                        close()
                        onEvent(AllExpensesUiEvent.OnPayerFilterPicked(payer))
                    }
                }
            }
            if (filters.isFiltering) {
                TextButton(onClick = { onEvent(AllExpensesUiEvent.OnClearFiltersClicked) }) {
                    Text(stringResource(R.string.groups_all_expenses_clear))
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    sort: ExpenseSort,
    onPick: (ExpenseSort) -> Unit,
) {
    MenuChip(
        label = sortLabel(sort),
        isSelected = false,
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = stringResource(R.string.groups_all_expenses_sort),
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    ) { close ->
        ExpenseSort.entries.forEach { option ->
            CheckedMenuItem(
                text = sortLabel(option),
                isChecked = sort == option,
            ) {
                close()
                onPick(option)
            }
        }
    }
}

// Why: several groups may be picked at once, so the menu stays open while they are.
@Composable
private fun GroupChip(
    state: AllExpensesUiState,
    onEvent: (AllExpensesUiEvent) -> Unit,
) {
    val picked = state.filters.groupIds
    MenuChip(
        label = state.groupOptions
            .singleOrNull { it.id in picked }
            ?.name
            ?.takeIf { picked.size == 1 }
            ?: buildCountedLabel(stringResource(R.string.groups_all_expenses_group), picked.size),
        isSelected = picked.isNotEmpty(),
    ) {
        state.groupOptions.forEach { group ->
            CheckedMenuItem(
                text = group.name,
                isChecked = group.id in picked,
                leading = {
                    GroupBadge(
                        initials = group.initials,
                        color = group.color,
                        photoUrl = group.photoUrl,
                        size = 24.dp,
                    )
                },
            ) { onEvent(AllExpensesUiEvent.OnGroupToggled(group.id)) }
        }
    }
}

@Composable
private fun CategoryChip(
    state: AllExpensesUiState,
    onEvent: (AllExpensesUiEvent) -> Unit,
) {
    val picked = state.filters.categoryIds
    MenuChip(
        label = state.categoryOptions
            .singleOrNull { it.id in picked }
            ?.let { categoryName(it) }
            ?.takeIf { picked.size == 1 }
            ?: buildCountedLabel(stringResource(R.string.groups_all_expenses_category), picked.size),
        isSelected = picked.isNotEmpty(),
    ) {
        state.categoryOptions.forEach { category ->
            CheckedMenuItem(
                text = categoryName(category),
                isChecked = category.id in picked,
                leading = {
                    CategoryBadge(
                        category = category,
                        size = 24.dp,
                    )
                },
            ) { onEvent(AllExpensesUiEvent.OnCategoryToggled(category.id)) }
        }
    }
}

// Why: "Group" with nothing picked, "Group · 2" with several; one picked shows its own name.
private fun buildCountedLabel(
    label: String,
    count: Int,
): String = if (count > 1) "$label · $count" else label

// Why: a chip that opens its menu under it; content gets a way to close it.
@Composable
private fun MenuChip(
    label: String,
    isSelected: Boolean,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = isSelected,
            onClick = { isOpen = true },
            label = { Text(label, maxLines = 1) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
        ) {
            content { isOpen = false }
        }
    }
}

@Composable
private fun CheckedMenuItem(
    text: String,
    isChecked: Boolean,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = leading,
        trailingIcon = {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun sortLabel(sort: ExpenseSort): String = stringResource(
    when (sort) {
        ExpenseSort.NEWEST -> R.string.groups_all_expenses_sort_newest
        ExpenseSort.OLDEST -> R.string.groups_all_expenses_sort_oldest
        ExpenseSort.LARGEST -> R.string.groups_all_expenses_sort_largest
        ExpenseSort.SMALLEST -> R.string.groups_all_expenses_sort_smallest
    },
)

@Composable
private fun shareLabel(share: ShareFilter): String = stringResource(
    when (share) {
        ShareFilter.ALL -> R.string.groups_all_expenses_share_all
        ShareFilter.TO_RECEIVE -> R.string.groups_all_expenses_share_receive
        ShareFilter.TO_PAY -> R.string.groups_all_expenses_share_pay
    },
)

@Composable
private fun payerLabel(payer: PayerFilter): String = stringResource(
    when (payer) {
        PayerFilter.ALL -> R.string.groups_all_expenses_payer_all
        PayerFilter.YOU -> R.string.groups_all_expenses_payer_you
        PayerFilter.OTHERS -> R.string.groups_all_expenses_payer_others
    },
)
