package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryBranch
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryPicker
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategorySuggestionReason
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

/*
 * Why: "Categoria" (7.7) in Rachar conta's place: full screen on a phone, in two columns lying down (the list, and the
 * subcategories of the one open beside it), a dialog over the group on a tablet, as the canvas draws it.
 */
@Composable
internal fun CategoryPickerContent(
    state: SplitBillUiState,
    picker: CategoryPicker,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val layout = rememberDeviceLayout()
    val usesTwoColumns = if (isDialog) layout.isTablet && layout.isLandscape else layout.isLandscape
    val branch = state.openCategoryBranch
    ColumnsFormLayout(
        chrome = FormChrome(
            title = stringResource(R.string.split_bill_category_title),
            subtitle = null,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.split_bill_back),
            onNavigate = { onEvent(SplitBillUiEvent.OnCategoryDismissed) },
            confirmLabel = stringResource(
                if (isDialog ||
                    usesTwoColumns
                ) {
                    R.string.split_bill_category_new_long
                } else {
                    R.string.split_bill_category_new
                },
            ),
            isConfirmEnabled = true,
            onConfirm = {
                onEvent(
                    SplitBillUiEvent.OnNewCategoryClicked(
                        parentId = null,
                        name = "",
                    ),
                )
            },
            cancelLabel = stringResource(R.string.split_bill_cancel),
            isNavigationAtEndInDialog = false,
            iconAction = null,
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            CategorySearchField(
                query = picker.query,
                onEvent = onEvent,
            )
            if (state.isSearchingCategories) {
                CategoryResultsList(
                    state = state,
                    query = picker.query.trim(),
                    onEvent = onEvent,
                )
            } else {
                SuggestedCategoriesSection(
                    state = state,
                    onEvent = onEvent,
                )
                CategoryMapCard(
                    state = state,
                    onEvent = onEvent,
                )
                SectionLabel(stringResource(R.string.split_bill_category_all))
                CategoryBranchesList(
                    state = state,
                    showsSubcategoriesInline = !usesTwoColumns,
                    onEvent = onEvent,
                )
            }
        },
        second = {
            if (usesTwoColumns && !state.isSearchingCategories && branch != null) {
                SubcategoriesPane(
                    state = state,
                    branch = branch,
                    onEvent = onEvent,
                )
            }
        },
    )
}

@Composable
private fun CategorySearchField(
    query: String,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = { onEvent(SplitBillUiEvent.OnCategoryQueryChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.split_bill_category_search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onEvent(SplitBillUiEvent.OnCategoryQueryChanged("")) }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.split_bill_category_clear_search),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: "Sugeridas", each with why ("Pela descrição"), and which is the group's most used.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestedCategoriesSection(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val suggestions = state.categorySuggestions
    if (suggestions.isEmpty()) return
    SectionLabel(stringResource(R.string.split_bill_category_suggested))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            CategoryChip(
                text = categoryNameOf(suggestion.category),
                isSelected = suggestion.isSelected,
                hint = when (suggestion.reason) {
                    CategorySuggestionReason.DESCRIPTION -> stringResource(
                        R.string.split_bill_category_reason_description,
                    )

                    CategorySuggestionReason.MOST_USED -> stringResource(R.string.split_bill_category_reason_most_used)

                    CategorySuggestionReason.GROUP -> stringResource(R.string.split_bill_category_reason_group)

                    CategorySuggestionReason.USED -> null
                },
                isGroupCategory = suggestion.category.isGroupCategory,
                onClick = { onEvent(SplitBillUiEvent.OnCategoryPicked(suggestion.category.id)) },
            )
        }
    }
    state.mostUsedCategory?.let { mostUsed ->
        Text(
            text = stringResource(
                R.string.split_bill_category_most_used_note,
                categoryNameOf(mostUsed),
                state.groupName,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: "Todas as categorias": each main one opens its subcategories, inline on a phone.
@Composable
private fun CategoryBranchesList(
    state: SplitBillUiState,
    showsSubcategoriesInline: Boolean,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val branches = state.categories.branches
    val openId = state.categoryPicker?.openMainId
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        branches.forEachIndexed { index, branch ->
            val isOpen = branch.category.id == openId
            Surface(
                shape = groupedRowShapeOf(index, branches.size),
                color = if (isOpen && !showsSubcategoriesInline) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
            ) {
                Column {
                    CategoryRow(
                        state = state,
                        branch = branch,
                        isOpen = isOpen,
                        showsChevron = !branch.category.isOther,
                        onEvent = onEvent,
                    )
                    if (isOpen && showsSubcategoriesInline) {
                        SubcategoryChipRow(
                            state = state,
                            branch = branch,
                            onEvent = onEvent,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 14.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryRow(
    state: SplitBillUiState,
    branch: CategoryBranch,
    isOpen: Boolean,
    showsChevron: Boolean,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val category = branch.category
    CategoryListRow(
        category = category,
        onClick = { onEvent(SplitBillUiEvent.OnCategoryMainToggled(category.id)) },
        onEvent = onEvent,
        trailing = {
            if (showsChevron) {
                Icon(
                    imageVector = if (isOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = categoryNameOf(category),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (category.isGroupCategory) GroupMarkIcon()
        }
        Text(
            text = branchSubtitleOf(state, branch),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: a category's row in the picker, its badge and what it says; a long press edits one of the group's (7.8e).
@Composable
private fun CategoryListRow(
    category: Category,
    onClick: () -> Unit,
    onEvent: (SplitBillUiEvent) -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .categoryClickable(
                category = category,
                editLabel = stringResource(R.string.split_bill_category_edit_action),
                onClick = onClick,
                onEdit = { onEvent(SplitBillUiEvent.OnEditCategoryClicked(category.id)) },
            ).padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category = category)
        Column(
            modifier = Modifier.weight(1f),
            content = content,
        )
        trailing()
    }
}

// Why: the bill's subcategory under its main one, "Criada por Carla · do grupo" for the group's, else how many it has.
@Composable
internal fun branchSubtitleOf(
    state: SplitBillUiState,
    branch: CategoryBranch,
): String {
    val category = branch.category
    val selected = branch.subcategories.firstOrNull { it.id == state.category.id }
    return when {
        selected != null -> categoryNameOf(selected)

        category.isGroupCategory -> creatorNameOrNull(state, category)
            ?.let { stringResource(R.string.split_bill_category_created_by_group, it) }
            ?: stringResource(R.string.split_bill_category_from_group)

        branch.subcategories.isEmpty() -> stringResource(R.string.split_bill_category_no_subcategories)

        else -> pluralStringResource(
            R.plurals.split_bill_category_subcategories,
            branch.subcategories.size,
            branch.subcategories.size,
        )
    }
}

// Why: the little people of a category created in the group.
@Composable
internal fun GroupMarkIcon() {
    Icon(
        imageVector = Icons.Outlined.Groups,
        contentDescription = stringResource(R.string.split_bill_category_from_group),
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: "2 resultados", each with where it is ("em Casa"), and "Criar categoria “alug”"; or nothing found (7.7c).
@Composable
private fun CategoryResultsList(
    state: SplitBillUiState,
    query: String,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val results = state.categoryResults
    if (results.isEmpty()) {
        NoCategoryFoundContent(
            state = state,
            query = query,
            onEvent = onEvent,
        )
        return
    }
    SectionLabel(pluralStringResource(R.plurals.split_bill_category_results, results.size, results.size))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        results.forEachIndexed { index, category ->
            ResultRow(
                state = state,
                category = category,
                index = index,
                count = results.size,
                onEvent = onEvent,
            )
        }
    }
    TextButton(onClick = {
        onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = query,
            ),
        )
    }) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.split_bill_category_create_query, query),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(
    state: SplitBillUiState,
    category: Category,
    index: Int,
    count: Int,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val parent = category.parentId?.let { state.categories.findOrNull(it) }
    Surface(
        shape = groupedRowShapeOf(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        CategoryListRow(
            category = category,
            onClick = { onEvent(SplitBillUiEvent.OnCategoryPicked(category.id)) },
            onEvent = onEvent,
        ) {
            Text(
                text = categoryNameOf(category),
                style = MaterialTheme.typography.bodyLarge,
            )
            parent?.let {
                Text(
                    text = stringResource(R.string.split_bill_category_in, categoryNameOf(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoCategoryFoundContent(
    state: SplitBillUiState,
    query: String,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.split_bill_category_none_title, query),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.split_bill_category_none_text, state.groupName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = {
            onEvent(
                SplitBillUiEvent.OnNewCategoryClicked(
                    parentId = null,
                    name = query,
                ),
            )
        }) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.split_bill_category_create_short, query),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// Why: a tap picks or opens it; a long press (and TalkBack's action) edits it, only for the group's own.
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.categoryClickable(
    category: Category,
    editLabel: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
): Modifier = semantics {
    if (category.isGroupCategory) customActions = listOf(CustomAccessibilityAction(editLabel) { onEdit().let { true } })
}.combinedClickable(
    onClick = onClick,
    onLongClick = if (category.isGroupCategory) onEdit else null,
)

@DevicePreviews
@Composable
private fun CategoryPickerContentPreview() {
    val state = createPreviewBillState()
    BlitzSplitTheme {
        CategoryPickerContent(
            state = state.copy(
                categoryPicker = CategoryPicker(
                    query = "",
                    openMainId = "food",
                ),
            ),
            picker = CategoryPicker(
                query = "",
                openMainId = "food",
            ),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryPickerContentSearchPreview() {
    val picker = CategoryPicker(
        query = "alug",
        openMainId = null,
    )
    BlitzSplitTheme {
        CategoryPickerContent(
            state = createPreviewBillState().copy(categoryPicker = picker),
            picker = picker,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
