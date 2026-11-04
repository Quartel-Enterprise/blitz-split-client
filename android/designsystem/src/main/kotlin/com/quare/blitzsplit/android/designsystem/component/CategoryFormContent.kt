package com.quare.blitzsplit.android.designsystem.component

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
import androidx.compose.runtime.Immutable
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
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.categoryIconLabel
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.format.imageVector
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryEditor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree

private val previewActions = CategoryFormActions(
    onNameChanged = {},
    onParentPicked = {},
    onIconPicked = {},
    onSave = {},
    onDismiss = {},
    onDelete = {},
)

// Why: what "Nova categoria" sends back, to a bill's ViewModel or the map's.
@Immutable
data class CategoryFormActions(
    val onNameChanged: (String) -> Unit,
    val onParentPicked: (String?) -> Unit,
    val onIconPicked: (CategoryIcon) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
    val onDelete: () -> Unit,
)

/*
 * Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" (7.8), in a bill's picker's place or over the map
 * (9.4): the name and where it goes on one side, the icon on the other when lying down, as the canvas draws it.
 * groupField is the map's "Grupo", above the name.
 */
@Composable
fun CategoryFormContent(
    editor: CategoryEditor,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    actions: CategoryFormActions,
    groupField: (@Composable () -> Unit)? = null,
) {
    val form = editor.form
    val title = stringResource(
        when {
            form.isEditing -> R.string.designsystem_category_form_edit
            form.isSubcategory -> R.string.designsystem_category_form_sub
            else -> R.string.designsystem_category_form_new
        },
    )
    ColumnsFormLayout(
        chrome = FormChrome(
            title = title,
            subtitle = null,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.designsystem_back),
            onNavigate = actions.onDismiss,
            confirmLabel = stringResource(R.string.designsystem_category_save),
            isConfirmEnabled = editor.canSave,
            onConfirm = actions.onSave,
            cancelLabel = stringResource(R.string.designsystem_confirm_cancel),
            isNavigationAtEndInDialog = false,
            iconAction = null,
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            groupField?.invoke()
            NameField(
                editor = editor,
                onNameChanged = actions.onNameChanged,
            )
            ParentField(
                editor = editor,
                onParentPicked = actions.onParentPicked,
            )
            PreviewRow(editor = editor)
            if (editor.showsOfflineNotice) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Outlined.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    NoteText(stringResource(R.string.designsystem_category_offline))
                }
            }
            NoteText(groupNoteOf(editor))
        },
        second = {
            if (!form.isSubcategory) {
                IconGrid(
                    selected = form.icon,
                    onIconPicked = actions.onIconPicked,
                )
            }
            if (form.isEditing) {
                TextButton(
                    onClick = actions.onDelete,
                    enabled = editor.canDelete,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.designsystem_category_delete),
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
    editor: CategoryEditor,
    onNameChanged: (String) -> Unit,
) {
    val form = editor.form
    val parent = editor.parent
    val help = when {
        editor.isNameTaken && parent != null -> stringResource(
            R.string.designsystem_category_name_taken_in,
            form.name.trim(),
            categoryName(parent),
        )

        editor.isNameTaken -> stringResource(R.string.designsystem_category_name_taken, form.name.trim())

        form.isEditing -> listOfNotNull(
            editor.creatorName?.let { stringResource(R.string.designsystem_category_created_by, it) },
            editor.usage?.let { pluralStringResource(R.plurals.designsystem_category_used_in, it, it) },
        ).joinToString(" · ")

        else -> stringResource(R.string.designsystem_category_name_help)
    }
    OutlinedTextField(
        value = form.name,
        onValueChange = onNameChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.designsystem_category_name)) },
        supportingText = { Text(help) },
        isError = editor.isNameTaken,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

// Why: "Dentro de": a main category, or inside one; a category with subcategories stays a main one.
@Composable
private fun ParentField(
    editor: CategoryEditor,
    onParentPicked: (String?) -> Unit,
) {
    val parent = editor.parent
    val help = when {
        editor.isParentLocked -> stringResource(R.string.designsystem_category_parent_locked)
        parent != null -> stringResource(R.string.designsystem_category_parent_help_sub, categoryName(parent))
        else -> stringResource(R.string.designsystem_category_parent_help)
    }
    val noneLabel = stringResource(R.string.designsystem_category_parent_none)
    CategoryDropdownField(
        label = stringResource(R.string.designsystem_category_parent),
        value = parent?.let { categoryName(it) } ?: noneLabel,
        help = help,
        isEnabled = !editor.isParentLocked,
        options = listOf(null to noneLabel) + editor.parents.map { it.id to categoryName(it) },
        onPicked = onParentPicked,
    )
}

/*
 * Why: a read-only field that opens its menu wherever it is tapped, the way a select does: "Dentro de", and the map's
 * "Grupo".
 */
@Composable
fun CategoryDropdownField(
    label: String,
    value: String,
    help: String?,
    isEnabled: Boolean,
    options: List<Pair<String?, String>>,
    onPicked: (String?) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            supportingText = help?.let { { Text(it) } },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = if (help != null) 24.dp else 0.dp)
                .clickable(
                    enabled = isEnabled,
                    role = Role.DropdownList,
                    onClick = { isExpanded = true },
                ),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        isExpanded = false
                        onPicked(id)
                    },
                )
            }
        }
    }
}

// Why: "Assim aparece nas contas: Alimentação › Feira", with the icon and color it gets.
@Composable
private fun PreviewRow(editor: CategoryEditor) {
    val isDark = LocalIsDarkTheme.current
    val parent = editor.parent
    val name = editor.form.name.trim()
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
                color = editor.color.toContainerColor(isDark),
                contentColor = editor.color.toContentColor(isDark),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        editor.form.icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column {
                NoteText(stringResource(R.string.designsystem_category_preview))
                Text(
                    text = if (parent != null) {
                        stringResource(R.string.designsystem_category_path, categoryName(parent), name)
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
    onIconPicked: (CategoryIcon) -> Unit,
) {
    Text(
        text = stringResource(R.string.designsystem_category_icon),
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
                        onClick = { onIconPicked(icon) },
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

// Why: "Fica para todo o Viagem Floripa…", or what an edit changes, with the bills that already use it once read.
@Composable
private fun groupNoteOf(editor: CategoryEditor): String {
    val usage = editor.usage
    return if (editor.form.isEditing && usage != null) {
        pluralStringResource(
            R.plurals.designsystem_category_edit_note,
            usage,
            editor.groupName,
            usage,
            editor.form.name.trim(),
        )
    } else {
        stringResource(R.string.designsystem_category_group_note, editor.groupName)
    }
}

// Why: "Apagar “Churrasco”?" says where its bills go (7.8f).
@Composable
fun DeleteCategoryDialog(
    deletion: CategoryDeletion,
    groupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = categoryName(deletion.category)
    ConfirmDialog(
        title = stringResource(R.string.designsystem_category_delete_title, name),
        message = if (deletion.billCount > 0) {
            pluralStringResource(
                R.plurals.designsystem_category_delete_text,
                deletion.billCount,
                deletion.billCount,
                name,
                categoryName(deletion.movesTo),
                groupName,
            )
        } else {
            stringResource(R.string.designsystem_category_delete_none, groupName)
        },
        confirmLabel = stringResource(R.string.designsystem_category_delete_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isBusy = deletion.isDeleting,
        placement = rememberDialogPlacement(),
    )
}

private fun buildPreviewEditor(form: CategoryForm): CategoryEditor = CategoryEditor(
    form = form,
    categories = CategoryTree.appOnly,
    groupName = "Viagem Floripa",
    creatorName = null,
    usage = 0,
    deletion = null,
    isOnline = false,
)

@DevicePreviews
@Composable
private fun CategoryFormContentPreview() {
    BlitzSplitTheme {
        CategoryFormContent(
            editor = buildPreviewEditor(
                CategoryForm.create(
                    categories = CategoryTree.appOnly,
                    groupId = "group",
                    parentId = null,
                    name = "Pet",
                ),
            ),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            actions = previewActions,
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryFormContentSubcategoryPreview() {
    BlitzSplitTheme {
        CategoryFormContent(
            editor = buildPreviewEditor(
                CategoryForm.create(
                    categories = CategoryTree.appOnly,
                    groupId = "group",
                    parentId = "food",
                    name = "Feira",
                ),
            ),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            actions = previewActions,
        )
    }
}
