package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val cardPadding = Modifier.padding(
    start = 16.dp,
    end = 16.dp,
    top = 16.dp,
)

// Why: "Nenhuma despesa ainda", with "Rachar conta" and "Convidar pessoas" in it.
@Composable
internal fun EmptyExpensesCard(onAction: (GroupAction) -> Unit) {
    GroupStateCard(
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        title = stringResource(R.string.group_detail_empty_title),
        message = stringResource(R.string.group_detail_empty_message),
    ) {
        Button(
            onClick = { onAction(GroupAction.SPLIT_BILL) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.group_detail_split_bill)) }
        FilledTonalButton(
            onClick = { onAction(GroupAction.INVITE) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.group_detail_invite)) }
    }
}

// Why: "Ninguém deve nada ainda", before the group's first bill.
@Composable
internal fun EmptyBalancesCard() {
    GroupStateCard(
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        title = stringResource(R.string.group_detail_balances_empty_title),
        message = stringResource(R.string.group_detail_balances_empty_message),
    )
}

// Why: "Todo mundo acertou", which offers deleting the group once the trip is over; offline it needs the internet.
@Composable
internal fun EveryoneSettledCard(
    canDelete: Boolean,
    onAction: (GroupAction) -> Unit,
) {
    GroupStateCard(
        icon = Icons.Outlined.Celebration,
        title = stringResource(R.string.group_detail_balances_settled_title),
        message = stringResource(R.string.group_detail_balances_settled_message),
    ) {
        OutlinedButton(
            onClick = { onAction(GroupAction.DELETE_GROUP) },
            enabled = canDelete,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.group_detail_delete_group))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupStateCard(
    icon: ImageVector,
    title: String,
    message: String,
    buttons: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Surface(
        modifier = cardPadding.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 24.dp,
                vertical = 32.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie4Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            buttons?.let { content ->
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content,
                )
            }
        }
    }
}
