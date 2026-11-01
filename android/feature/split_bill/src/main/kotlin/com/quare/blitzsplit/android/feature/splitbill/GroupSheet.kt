package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

// Why: "Rachar em qual grupo?" marks a group until "Continuar"; dismissing it leaves the bill in its group.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupSheet(
    groups: List<BillGroup>,
    selection: GroupSelection,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(SplitBillUiEvent.OnGroupDismissed) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        GroupColumn(
            groups = groups,
            selection = selection,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun GroupColumn(
    groups: List<BillGroup>,
    selection: GroupSelection,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    SheetColumn(
        title = stringResource(R.string.split_bill_group_title),
        subtitle = stringResource(R.string.split_bill_group_lead),
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            groups.forEachIndexed { index, group ->
                SheetOptionRow(
                    title = group.name,
                    subtitle = pluralStringResource(
                        R.plurals.split_bill_group_people,
                        group.memberCount,
                        group.memberCount,
                    ),
                    isSelected = group.id == selection.groupId,
                    shape = groupedRowShapeOf(index, groups.size),
                    onClick = { onEvent(SplitBillUiEvent.OnGroupPicked(group.id)) },
                ) {
                    GroupBadge(
                        initials = group.initials,
                        color = group.color,
                        size = 40.dp,
                    )
                }
            }
        }
        Button(
            onClick = { onEvent(SplitBillUiEvent.OnGroupDone) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.split_bill_group_continue)) }
    }
}

@DevicePreviews
@Composable
private fun GroupColumnPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            val state = createPreviewBillState()
            GroupColumn(
                groups = state.groups,
                selection = GroupSelection(groupId = state.groupId),
                onEvent = {},
            )
        }
    }
}
