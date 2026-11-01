package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermError
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermForm

private val landscapeSheetMaxWidth = 640.dp
private val dialogWidth = 560.dp

/*
 * Why: "Novo termo" and "Editar termo" (9.2): a bottom sheet on a phone (centered at up to 640dp lying down), a 560dp
 * dialog on a tablet and a foldable upright, and the book's right half with the map under the scrim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TermSheet(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val form = state.termForm ?: return
    val onDismiss = { onEvent(CategoryMapUiEvent.OnTermDismissed) }
    val formFactor = rememberFoldLayout().formFactor
    if (formFactor == FormFactor.Phone || formFactor == FormFactor.PhoneLandscape) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
            sheetMaxWidth = landscapeSheetMaxWidth,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            TermFormContent(
                state = state,
                form = form,
                onEvent = onEvent,
            )
        }
    } else {
        FoldDialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = dialogWidth),
            ) {
                TermFormContent(
                    state = state,
                    form = form,
                    onEvent = onEvent,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun TermFormContent(
    state: CategoryMapUiState,
    form: TermForm,
    onEvent: (CategoryMapUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(
                if (form.isEditing) R.string.category_map_edit_term else R.string.category_map_new_term,
            ),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        TermTextField(
            state = state,
            form = form,
            onEvent = onEvent,
        )
        TermCategoryField(
            state = state,
            form = form,
            onEvent = onEvent,
        )
        TermExampleText(
            state = state,
            form = form,
        )
        TermButtonsRow(
            state = state,
            form = form,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun TermTextField(
    state: CategoryMapUiState,
    form: TermForm,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val duplicate = state.duplicateTerm
    val duplicateCategory = state.duplicateCategory
    val help = when {
        state.termError == TermError.TOO_SHORT -> stringResource(R.string.category_map_term_too_short)

        state.termError == TermError.INVALID_CHARACTERS -> stringResource(R.string.category_map_term_invalid)

        duplicate != null && duplicateCategory != null -> stringResource(
            R.string.category_map_term_duplicate,
            duplicate.term,
            pathOf(duplicateCategory, state),
        )

        form.original?.isAppTerm == true -> stringResource(R.string.category_map_term_help_app)

        else -> stringResource(R.string.category_map_term_help)
    }
    OutlinedTextField(
        value = form.text,
        onValueChange = { onEvent(CategoryMapUiEvent.OnTermTextChanged(it)) },
        label = { Text(stringResource(R.string.category_map_term_label)) },
        supportingText = { Text(help) },
        isError = state.termError != null || duplicate != null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TermCategoryField(
    state: CategoryMapUiState,
    form: TermForm,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val category = state.formCategory ?: return
    val help = when {
        state.duplicateCategory != null -> stringResource(
            R.string.category_map_category_help_move,
            categoryName(state.duplicateCategory ?: category),
        )

        form.isEditing -> stringResource(R.string.category_map_category_help_edit)

        else -> stringResource(R.string.category_map_category_help)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.category_map_category),
            style = MaterialTheme.typography.labelLarge,
        )
        CategoryOptionRow(
            category = category,
            label = pathOf(category, state),
            isSelected = false,
            trailing = {
                Icon(
                    if (form.isPickingCategory) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    null,
                )
            },
            onClick = { onEvent(CategoryMapUiEvent.OnTermCategoryClicked) },
        )
        if (form.isPickingCategory) {
            CategoryOptionsList(
                state = state,
                form = form,
                onEvent = onEvent,
            )
        }
        Text(
            text = help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.CategoryOptionsList(
    state: CategoryMapUiState,
    form: TermForm,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    state.categoryOptions.forEach { option ->
        CategoryOptionRow(
            category = option,
            label = optionLabelOf(option, state),
            isSelected = option.id == form.categoryId,
            modifier = Modifier.padding(start = if (option.parentId != null) 24.dp else 0.dp),
            onClick = { onEvent(CategoryMapUiEvent.OnTermCategoryPicked(option.id)) },
        )
    }
}

@Composable
private fun optionLabelOf(
    option: Category,
    state: CategoryMapUiState,
): String {
    val name = categoryName(option)
    val group = state.groupNameOf(option) ?: return name
    return name + " · " + stringResource(R.string.category_map_only_in_group, group)
}

@Composable
private fun CategoryOptionRow(
    category: Category,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            category = category,
            size = 32.dp,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null)
        trailing()
    }
}

// Why: "Exemplo: “Pizzaria” → Alimentação › Restaurante", "Se mover" for a word another category had.
@Composable
private fun TermExampleText(
    state: CategoryMapUiState,
    form: TermForm,
) {
    val category = state.formCategory ?: return
    val text = form.text.trim()
    if (text.isEmpty()) return
    val label = stringResource(
        if (state.duplicateTerm !=
            null
        ) {
            R.string.category_map_example_move
        } else {
            R.string.category_map_example
        },
    )
    Text(
        text = "$label: “${text.replaceFirstChar { it.uppercaseChar() }}” → ${pathOf(category, state)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TermButtonsRow(
    state: CategoryMapUiState,
    form: TermForm,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (form.isEditing) {
            TextButton(onClick = { onEvent(CategoryMapUiEvent.OnTermRemoveClicked) }) {
                Text(
                    text = stringResource(R.string.category_map_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(modifier = Modifier.weight(1f)) {}
        TextButton(onClick = { onEvent(CategoryMapUiEvent.OnTermDismissed) }) {
            Text(stringResource(R.string.category_map_cancel))
        }
        Button(
            onClick = { onEvent(CategoryMapUiEvent.OnTermSaveClicked) },
            enabled = state.canSaveTerm,
        ) {
            val target = state.formCategory
            Text(
                if (state.duplicateTerm != null && target != null) {
                    stringResource(R.string.category_map_move_to, categoryName(target))
                } else {
                    stringResource(R.string.category_map_save)
                },
            )
        }
    }
}

@Composable
internal fun pathOf(
    category: Category,
    state: CategoryMapUiState,
): String {
    val parent = category.parentId?.let { state.categories.findOrNull(it) }
    return parent?.let { stringResource(R.string.category_map_path, categoryName(it), categoryName(category)) }
        ?: categoryName(category)
}
