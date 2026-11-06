package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.simplifyDebtsIcon
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyCurrencyChange
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsConfirmation
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyPersonChange

private val changeFaceSize = 28.dp

/*
 * Why: "Simplificar dívidas?" (13.3), "Desligar Simplificar dívidas?" (13.3b), the already minimal group (13.3c) and
 * the payments in the queue (13.3d), over Editar grupo, off the hinge as every dialog: the payments before and after,
 * what it does, what changes for each person, and who gets told.
 */
@Composable
internal fun SimplifyDebtsDialog(
    confirmation: SimplifyDebtsConfirmation,
    isChanging: Boolean,
    onEvent: (EditGroupUiEvent) -> Unit,
) {
    FoldAlertDialog(
        onDismissRequest = { if (!isChanging) onEvent(EditGroupUiEvent.OnSimplifyDebtsDismissed) },
        title = confirmationTitleOf(confirmation),
        icon = {
            Icon(
                imageVector = simplifyDebtsIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        text = {
            SimplifyDebtsDialogColumn(confirmation)
        },
        confirmButton = {
            Button(
                onClick = { onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed) },
                enabled = !isChanging,
            ) {
                if (isChanging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(confirmLabelOf(confirmation))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(EditGroupUiEvent.OnSimplifyDebtsDismissed) },
                enabled = !isChanging,
            ) { Text(dismissLabelOf(confirmation)) }
        },
        placement = rememberDialogPlacement(),
    )
}

@Composable
private fun SimplifyDebtsDialogColumn(confirmation: SimplifyDebtsConfirmation) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        confirmation.currencies.forEach { change ->
            PaymentCountsRow(
                change = change,
                isTurningOn = confirmation.isTurningOn,
                showsCurrency = confirmation.currencies.size > 1,
            )
        }
        Text(confirmationBodyOf(confirmation))
        if (confirmation.people.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.simplify_confirm_what_changes),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                confirmation.people.forEach { change ->
                    PersonChangeRow(
                        change = change,
                        showsCurrency = confirmation.currencies.size > 1,
                    )
                }
            }
        }
        if (confirmation.queuedPayments.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 10.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PendingIndicator(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = queuedTextOf(confirmation.queuedPayments),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Text(
            text = confirmationFootOf(confirmation),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// Why: "7 pagamentos → 5 pagamentos" of one currency, the new count in the primary color while turning on saves some.
@Composable
private fun PaymentCountsRow(
    change: SimplifyCurrencyChange,
    isTurningOn: Boolean,
    showsCurrency: Boolean,
) {
    val before = pluralStringResource(R.plurals.simplify_payments, change.paymentsBefore, change.paymentsBefore)
    val after = pluralStringResource(R.plurals.simplify_payments, change.paymentsAfter, change.paymentsAfter)
    val counts = stringResource(R.string.simplify_confirm_counts, before, after)
    val description = if (showsCurrency) "${change.currency.code}: $counts" else counts
    val highlightsAfter = isTurningOn && !change.isAlreadyMinimal
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showsCurrency) {
            Text(
                text = change.currency.code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaymentCountBox(
                count = change.paymentsBefore,
                container = MaterialTheme.colorScheme.surfaceContainerLowest,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PaymentCountBox(
                count = change.paymentsAfter,
                container = if (highlightsAfter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
                content = if (highlightsAfter) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PaymentCountBox(
    count: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 10.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = pluralStringResource(R.plurals.simplify_payments_label, count),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Why: the face, the name in bold and what changes for them, one sentence after the other.
@Composable
private fun PersonChangeRow(
    change: SimplifyPersonChange,
    showsCurrency: Boolean,
) {
    val name = changeNameOf(change)
    val text = changeTextOf(change)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarBadge(
            initial = change.initials,
            color = change.avatarColor,
            photoUrl = change.photoUrl,
            size = changeFaceSize,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) { append(name) }
                if (showsCurrency) append(" · ${change.currency.code}")
                append(" ")
                append(text)
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
