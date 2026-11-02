package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.ChoiceChipSurface
import com.quare.blitzsplit.core.expenses.domain.model.CategoryBranch
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: lying down and on a tablet, the open category's subcategories beside the list.
@Composable
internal fun SubcategoriesPane(
    state: SplitBillUiState,
    branch: CategoryBranch,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(category = branch.category)
                Column {
                    Text(
                        text = categoryNameOf(branch.category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.split_bill_category_side_hint, categoryNameOf(branch.category)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SubcategoryChipRow(
                state = state,
                branch = branch,
                onEvent = onEvent,
            )
            Text(
                text = stringResource(R.string.split_bill_category_hold_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "Só Alimentação", its subcategories (a long press edits the group's) and "Nova em Alimentação".
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubcategoryChipRow(
    state: SplitBillUiState,
    branch: CategoryBranch,
    onEvent: (SplitBillUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val main = branch.category
    val mainName = categoryNameOf(main)
    val editLabel = stringResource(R.string.split_bill_category_edit_action)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(
            text = stringResource(R.string.split_bill_category_only, mainName),
            isSelected = state.category.id == main.id,
            hint = null,
            isGroupCategory = false,
            onClick = { onEvent(SplitBillUiEvent.OnCategoryPicked(main.id)) },
        )
        branch.subcategories.forEach { subcategory ->
            CategoryChip(
                text = categoryNameOf(subcategory),
                isSelected = state.category.id == subcategory.id,
                hint = null,
                isGroupCategory = subcategory.isGroupCategory,
                onClick = { onEvent(SplitBillUiEvent.OnCategoryPicked(subcategory.id)) },
                onEdit = if (subcategory.isGroupCategory) {
                    { onEvent(SplitBillUiEvent.OnEditCategoryClicked(subcategory.id)) }
                } else {
                    null
                },
                editLabel = editLabel,
            )
        }
        Surface(
            onClick = {
                onEvent(
                    SplitBillUiEvent.OnNewCategoryClicked(
                        parentId = main.id,
                        name = "",
                    ),
                )
            },
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .padding(
                        start = 8.dp,
                        end = 12.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.split_bill_category_new_in, mainName),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryChip(
    text: String,
    isSelected: Boolean,
    hint: String?,
    isGroupCategory: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    editLabel: String = "",
) {
    ChoiceChipSurface(
        isSelected = isSelected,
        modifier = Modifier
            .semantics {
                selected = isSelected
                onEdit?.let { edit ->
                    customActions = listOf(CustomAccessibilityAction(editLabel) { edit().let { true } })
                }
            }.combinedClickable(
                role = Role.RadioButton,
                onClick = onClick,
                onLongClick = onEdit,
            ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .padding(
                    start = if (isSelected) 8.dp else 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
                hint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isGroupCategory) GroupMarkIcon()
        }
    }
}
