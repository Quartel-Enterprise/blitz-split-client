package com.quare.blitzsplit.android.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesGroupDialog
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityGroupOption
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent

private val filterWidth = 560.dp

/*
 * Why: "Filtrar atividade": one group or all, any types, then "Mostrar 4 resultados". A bottom sheet on a phone
 * (560dp wide lying down), a 560dp dialog on a tablet, in the right half of an unfolded book.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityFilterSheet(
    draft: ActivityFilter,
    resultCount: Int,
    groups: List<ActivityGroupOption>,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    val onDismiss = { onEvent(ActivityUiEvent.OnFilterDismissed) }
    if (rememberUsesGroupDialog()) {
        FoldDialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.widthIn(max = filterWidth),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                FilterColumn(
                    draft = draft,
                    resultCount = resultCount,
                    groups = groups,
                    onEvent = onEvent,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetMaxWidth = filterWidth,
        ) {
            FilterColumn(
                draft = draft,
                resultCount = resultCount,
                groups = groups,
                onEvent = onEvent,
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            )
        }
    }
}

@Composable
private fun FilterColumn(
    draft: ActivityFilter,
    resultCount: Int,
    groups: List<ActivityGroupOption>,
    onEvent: (ActivityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.activity_filter_title),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.activity_filter_group))
            Column(modifier = Modifier.selectableGroup()) {
                GroupOption(
                    label = stringResource(R.string.activity_filter_all_groups),
                    group = null,
                    isSelected = draft.groupId == null,
                    onClick = { onEvent(ActivityUiEvent.OnDraftGroupPicked(null)) },
                )
                groups.forEach { group ->
                    GroupOption(
                        label = group.name,
                        group = group,
                        isSelected = draft.groupId == group.id,
                        onClick = { onEvent(ActivityUiEvent.OnDraftGroupPicked(group.id)) },
                    )
                }
            }
            SectionTitle(stringResource(R.string.activity_filter_type))
            ActivityType.entries.forEach { type ->
                TypeOption(
                    type = type,
                    isChecked = type in draft.types,
                    onToggle = { onEvent(ActivityUiEvent.OnDraftTypeToggled(type)) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            OutlinedButton(onClick = { onEvent(ActivityUiEvent.OnFilterCleared) }) {
                Text(stringResource(R.string.activity_filter_clear))
            }
            Button(onClick = { onEvent(ActivityUiEvent.OnFilterApplied) }) {
                Text(pluralStringResource(R.plurals.activity_show_results, resultCount, resultCount))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(
                top = 16.dp,
                bottom = 4.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun GroupOption(
    label: String,
    group: ActivityGroupOption?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
            ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        if (group != null) {
            GroupDotIcon(
                color = group.color,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Box(modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TypeOption(
    type: ActivityType,
    isChecked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isChecked,
                onValueChange = { onToggle() },
                role = Role.Checkbox,
            ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
        )
        Icon(
            imageVector = iconOf(type),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = typeLabelOf(type),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
