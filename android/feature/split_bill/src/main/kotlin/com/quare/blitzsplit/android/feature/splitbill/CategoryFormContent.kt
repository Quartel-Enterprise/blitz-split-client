package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.android.designsystem.format.categoryIconLabel
import com.quare.blitzsplit.android.designsystem.format.imageVector
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryDeletion
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryForm
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

/*
 * Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" (7.8) in the picker's place: the name and where it
 * goes on one side, the icon on the other when lying down, as the canvas draws it.
 */
@Composable
internal fun CategoryFormContent(
    state: SplitBillUiState,
    form: CategoryForm,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val title = stringResource(
        when {
            form.isEditing -> R.string.split_bill_category_form_edit
            form.isSubcategory -> R.string.split_bill_category_form_sub
            else -> R.string.split_bill_category_form_new
        },
    )
    ColumnsFormLayout(
        chrome = FormChrome(
            title = title,
            subtitle = null,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.split_bill_back),
            onNavigate = { onEvent(SplitBillUiEvent.OnCategoryFormDismissed) },
            confirmLabel = stringResource(R.string.split_bill_save),
            isConfirmEnabled = state.canSaveCategory,
            onConfirm = { onEvent(SplitBillUiEvent.OnCategoryFormSaveClicked) },
            cancelLabel = stringResource(R.string.split_bill_cancel),
            isNavigationAtEndInDialog = false,
            iconAction = null,
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            NameField(
                state = state,
                form = form,
                onEvent = onEvent,
            )
            ParentField(
                state = state,
                form = form,
                onEvent = onEvent,
            )
            PreviewRow(
                state = state,
                form = form,
            )
            if (state.showsOfflineNotice) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Outlined.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    NoteText(stringResource(R.string.split_bill_category_offline))
                }
            }
            NoteText(groupNoteOf(state, form))
        },
        second = {
            if (!form.isSubcategory) {
                IconGrid(
                    selected = form.icon,
                    onEvent = onEvent,
                )
            }
            if (form.isEditing) {
                TextButton(onClick = { onEvent(SplitBillUiEvent.OnCategoryDeleteClicked) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.split_bill_category_delete),
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Composable
private fun NameField(
    state: SplitBillUiState,
    form: CategoryForm,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val parent = form.parentId?.let { state.categories.findOrNull(it) }
    val help = when {
        state.isCategoryNameTaken && parent != null -> stringResource(
            R.string.split_bill_category_name_taken_in,
            form.name.trim(),
            categoryNameOf(parent),
        )

        state.isCategoryNameTaken -> stringResource(R.string.split_bill_category_name_taken, form.name.trim())

        form.isEditing -> listOfNotNull(
            state.categories.findOrNull(form.categoryId)?.let { creatorNameOrNull(state, it) }?.let {
                stringResource(R.string.split_bill_category_created_by, it)
            },
            pluralStringResource(
                R.plurals.split_bill_category_used_in,
                state.categoryFormUsage,
                state.categoryFormUsage,
            ),
        ).joinToString(" · ")

        else -> stringResource(R.string.split_bill_category_name_help)
    }
    OutlinedTextField(
        value = form.name,
        onValueChange = { onEvent(SplitBillUiEvent.OnCategoryNameChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.split_bill_category_name)) },
        supportingText = { Text(help) },
        isError = state.isCategoryNameTaken,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

// Why: "Dentro de": a main category, or inside one; a category with subcategories stays a main one.
@Composable
private fun ParentField(
    state: SplitBillUiState,
    form: CategoryForm,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val parent = form.parentId?.let { state.categories.findOrNull(it) }
    val noneLabel = stringResource(R.string.split_bill_category_parent_none)
    val help = when {
        state.isCategoryParentLocked -> stringResource(R.string.split_bill_category_parent_locked)
        parent != null -> stringResource(R.string.split_bill_category_parent_help_sub, categoryNameOf(parent))
        else -> stringResource(R.string.split_bill_category_parent_help)
    }
    Box {
        OutlinedTextField(
            value = parent?.let { categoryNameOf(it) } ?: noneLabel,
            onValueChange = {},
            readOnly = true,
            enabled = !state.isCategoryParentLocked,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.split_bill_category_parent)) },
            supportingText = { Text(help) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
        )
        // Why: the read-only field opens the menu wherever it is tapped, the way a select does.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = 24.dp)
                .clickable(
                    enabled = !state.isCategoryParentLocked,
                    role = Role.DropdownList,
                    onClick = { isExpanded = true },
                ),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = {
                    isExpanded = false
                    onEvent(SplitBillUiEvent.OnCategoryParentPicked(null))
                },
            )
            state.categoryParents.forEach { option ->
                DropdownMenuItem(
                    text = { Text(categoryNameOf(option)) },
                    onClick = {
                        isExpanded = false
                        onEvent(SplitBillUiEvent.OnCategoryParentPicked(option.id))
                    },
                )
            }
        }
    }
}

// Why: "Assim aparece nas contas: Alimentação › Feira", with the icon it gets.
@Composable
private fun PreviewRow(
    state: SplitBillUiState,
    form: CategoryForm,
) {
    val isDark = LocalIsDarkTheme.current
    val parent = form.parentId?.let { state.categories.findOrNull(it) }
    val color = parent?.color ?: state.categories.findOrNull(form.categoryId)?.color ?: state.category.color
    val name = form.name.trim()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.toContainerColor(isDark),
                contentColor = color.toContentColor(isDark),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        form.icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column {
                NoteText(stringResource(R.string.split_bill_category_preview))
                Text(
                    text = if (parent != null) {
                        stringResource(
                            com.quare.blitzsplit.android.designsystem.R.string.designsystem_category_path,
                            categoryNameOf(parent),
                            name,
                        )
                    } else {
                        name
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// Why: the 18 icons a main category may take (7.8), a radio group for TalkBack.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid(
    selected: CategoryIcon,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Text(
        text = stringResource(R.string.split_bill_category_icon),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            val label = categoryIconLabel(icon)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = label }
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onEvent(SplitBillUiEvent.OnCategoryIconPicked(icon)) },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon.imageVector, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun NoteText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: "Fica para todo o Viagem Floripa…", or what an edit changes, with the bills that already use it.
@Composable
private fun groupNoteOf(
    state: SplitBillUiState,
    form: CategoryForm,
): String = if (form.isEditing) {
    pluralStringResource(
        R.plurals.split_bill_category_edit_note,
        state.categoryFormUsage,
        state.groupName,
        state.categoryFormUsage,
        form.name.trim(),
    )
} else {
    stringResource(R.string.split_bill_category_group_note, state.groupName)
}

// Why: "Apagar “Churrasco”?" says where its bills go (7.8f).
@Composable
internal fun DeleteCategoryDialog(
    state: SplitBillUiState,
    deletion: CategoryDeletion,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val name = categoryNameOf(deletion.category)
    ConfirmDialog(
        title = stringResource(R.string.split_bill_category_delete_title, name),
        message = if (deletion.billCount > 0) {
            pluralStringResource(
                R.plurals.split_bill_category_delete_text,
                deletion.billCount,
                deletion.billCount,
                name,
                categoryNameOf(deletion.movesTo),
                state.groupName,
            )
        } else {
            stringResource(R.string.split_bill_category_delete_none, state.groupName)
        },
        confirmLabel = stringResource(R.string.split_bill_category_delete_confirm),
        onConfirm = { onEvent(SplitBillUiEvent.OnCategoryDeleteConfirmed) },
        onDismiss = { onEvent(SplitBillUiEvent.OnCategoryDeleteDismissed) },
        isBusy = deletion.isDeleting,
    )
}

@DevicePreviews
@Composable
private fun CategoryFormContentPreview() {
    val form = CategoryForm(
        categoryId = null,
        name = "Pet",
        parentId = null,
        icon = CategoryIcon.PAW,
        createdByMemberId = null,
        isSaving = false,
    )
    BlitzSplitTheme {
        CategoryFormContent(
            state = createPreviewBillState(isOnline = false).copy(categoryForm = form),
            form = form,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryFormContentSubcategoryPreview() {
    val form = CategoryForm(
        categoryId = null,
        name = "Feira",
        parentId = "food",
        icon = CategoryIcon.FOOD,
        createdByMemberId = null,
        isSaving = false,
    )
    BlitzSplitTheme {
        CategoryFormContent(
            state = createPreviewBillState().copy(categoryForm = form),
            form = form,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
