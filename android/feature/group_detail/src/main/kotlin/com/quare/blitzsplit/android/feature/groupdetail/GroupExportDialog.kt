package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.component.nameRes
import com.quare.blitzsplit.android.designsystem.format.appCategoryName
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.export.GroupExportFormat
import com.quare.blitzsplit.core.expenses.export.GroupExportWords
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: "Export group data" asks which file to make; a format picked makes it from what the device has and opens the
 * system's save sheet. The file's headers come in the app's language, from here.
 */
@Composable
internal fun GroupExportDialog(onEvent: (GroupDetailUiEvent) -> Unit) {
    val words = groupExportWords()
    val onDismiss = { onEvent(GroupDetailUiEvent.OnExportDismissed) }
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.group_detail_export),
        placement = rememberDialogPlacement(),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(DesignR.string.designsystem_confirm_cancel))
            }
        },
        text = {
            Column {
                GroupExportFormat.entries.forEach { format ->
                    FormatRow(
                        icon = iconOf(format),
                        label = labelOf(format),
                        onClick = { onEvent(GroupDetailUiEvent.OnExportFormatPicked(format, words)) },
                    )
                }
            }
        },
    )
}

@Composable
private fun FormatRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun iconOf(format: GroupExportFormat): ImageVector = when (format) {
    GroupExportFormat.CSV -> Icons.Outlined.TableChart
    GroupExportFormat.XLSX -> Icons.Outlined.GridOn
    GroupExportFormat.JSON -> Icons.Outlined.DataObject
    GroupExportFormat.TXT -> Icons.Outlined.Description
}

@Composable
private fun labelOf(format: GroupExportFormat): String = stringResource(
    when (format) {
        GroupExportFormat.CSV -> R.string.group_detail_export_csv
        GroupExportFormat.XLSX -> R.string.group_detail_export_xlsx
        GroupExportFormat.JSON -> R.string.group_detail_export_json
        GroupExportFormat.TXT -> R.string.group_detail_export_text
    },
)

// Why: the file's headers and the app categories' names, in the app's language, as the shared code takes them.
@Composable
private fun groupExportWords(): GroupExportWords = GroupExportWords(
    bills = stringResource(R.string.group_detail_export_bills),
    payments = stringResource(R.string.group_detail_export_payments),
    balances = stringResource(R.string.group_detail_export_balances),
    date = stringResource(R.string.group_detail_export_date),
    billDescription = stringResource(R.string.group_detail_export_description),
    category = stringResource(R.string.group_detail_export_category),
    amount = stringResource(R.string.group_detail_export_amount),
    currency = stringResource(R.string.group_detail_export_currency),
    paid = stringResource(R.string.group_detail_export_paid),
    share = stringResource(R.string.group_detail_export_share),
    from = stringResource(R.string.group_detail_export_from),
    to = stringResource(R.string.group_detail_export_to),
    member = stringResource(R.string.group_detail_export_member),
    balance = stringResource(R.string.group_detail_export_balance),
    formerMember = stringResource(DesignR.string.designsystem_former_member),
    appCategoryNames = AppCategory.entries.associate { it.key to appCategoryName(it) },
    paymentMethod = stringResource(R.string.group_export_payment_method),
    payingMethodTypeNames = PayingMethodType.entries.associate { it.code to stringResource(it.nameRes) },
)
