package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.alertDialogPane
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.feature.contacts.presentation.model.AddGroupOption
import com.quare.blitzsplit.feature.contacts.presentation.model.AddToGroupDraft
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val sheetMaxWidth = 640.dp
private val dialogWidth = 560.dp

/*
 * Why: the ⋮ menu: Juntar for a guest (disabled offline, "Precisa de internet"), "Ver em Saldos", and "Ocultar de
 * Contatos" (only at zero, else disabled with why) or "Mostrar em Contatos" for someone hidden.
 */
@Composable
internal fun ContactProfileMenu(
    state: ContactProfileUiState,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val pick: (ContactProfileUiEvent) -> Unit = { event ->
        isExpanded = false
        onEvent(event)
    }
    IconButton(onClick = { isExpanded = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.contacts_profile_more),
        )
    }
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
    ) {
        if (state.contact?.isGuest == true) {
            MenuItem(
                label = stringResource(R.string.contacts_menu_merge),
                note = stringResource(
                    DesignR.string.designsystem_group_exit_needs_internet,
                ).takeUnless { state.canMerge },
                isEnabled = state.canMerge,
                onClick = { pick(ContactProfileUiEvent.OnMergeClicked) },
            )
        }
        MenuItem(
            label = stringResource(R.string.contacts_menu_view_balances),
            note = null,
            isEnabled = true,
            onClick = { pick(ContactProfileUiEvent.OnViewBalancesClicked) },
        )
        if (state.canShow) {
            MenuItem(
                label = stringResource(R.string.contacts_menu_show),
                note = null,
                isEnabled = true,
                onClick = { pick(ContactProfileUiEvent.OnShowClicked) },
            )
        } else {
            MenuItem(
                label = stringResource(R.string.contacts_menu_hide),
                note = stringResource(R.string.contacts_menu_hide_blocked).takeUnless { state.canHide },
                isEnabled = state.canHide,
                onClick = { pick(ContactProfileUiEvent.OnHideClicked) },
            )
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    note: String?,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label)
                note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        onClick = onClick,
        enabled = isEnabled,
    )
}

// Why: "Ocultar Felipe?": only a settled person, who stays in the groups and comes back under Ocultos.
@Composable
internal fun HideContactDialog(
    contact: Contact,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val title = stringResource(R.string.contacts_hide_title, contact.name)
    val onDismiss = { onEvent(ContactProfileUiEvent.OnHideDismissed) }
    FoldDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.alertDialogPane(title),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        R.string.contacts_hide_message,
                        contact.name,
                        nameListOf(contact.groups.map { it.groupName }),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(DesignR.string.designsystem_confirm_cancel))
                    }
                    TextButton(onClick = { onEvent(ContactProfileUiEvent.OnHideConfirmed) }) {
                        Text(stringResource(R.string.contacts_hide_confirm))
                    }
                }
            }
        }
    }
}

// Why: "Adicionar Carla a um grupo" (11.5): the groups the person is already in show disabled.
@Composable
internal fun AddToGroupSheet(
    contact: Contact,
    draft: AddToGroupDraft,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val onDismiss = { onEvent(ContactProfileUiEvent.OnAddToGroupDismissed) }
    ProfileSheet(
        title = stringResource(R.string.contacts_add_title, contact.name),
        onDismiss = onDismiss,
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            draft.options.forEachIndexed { index, option ->
                AddGroupRow(
                    option = option,
                    isSelected = option.groupId == draft.selectedGroupId,
                    index = index,
                    count = draft.options.size,
                    onClick = { onEvent(ContactProfileUiEvent.OnAddGroupPicked(option.groupId)) },
                )
            }
        }
        Text(
            text = stringResource(
                if (contact.isGuest) R.string.contacts_add_note_guest else R.string.contacts_add_note,
                contact.name,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(DesignR.string.designsystem_confirm_cancel))
            }
            Button(
                onClick = { onEvent(ContactProfileUiEvent.OnAddToGroupConfirmed) },
                enabled = draft.canAdd,
            ) {
                Text(stringResource(R.string.contacts_add_confirm))
            }
        }
    }
}

@Composable
private fun AddGroupRow(
    option: AddGroupOption,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val isEnabled = !option.isAlreadyIn
    Surface(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = groupedRowShapeOf(index, count),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupBadge(
                initials = option.initials,
                color = option.color,
                size = 36.dp,
            )
            val contentColor = if (isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                Text(
                    text = if (option.isAlreadyIn) {
                        stringResource(R.string.contacts_add_already_in)
                    } else {
                        pluralStringResource(R.plurals.contacts_add_members, option.memberCount, option.memberCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
            )
        }
    }
}

/*
 * Why: what the profile opens over itself: a bottom sheet on a phone (centered, at most 640dp, lying down), a 560dp
 * dialog elsewhere, in the half after the hinge on a book (FoldDialog).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val formFactor = rememberFoldLayout().formFactor
    val isPhone = formFactor == FormFactor.Phone || formFactor == FormFactor.PhoneLandscape
    if (isPhone) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetMaxWidth = sheetMaxWidth,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            SheetColumn(
                title = title,
                content = content,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
            )
        }
    } else {
        FoldDialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .widthIn(max = dialogWidth)
                    .fillMaxWidth()
                    .semantics { paneTitle = title },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                SheetColumn(
                    title = title,
                    content = content,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetColumn(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}
