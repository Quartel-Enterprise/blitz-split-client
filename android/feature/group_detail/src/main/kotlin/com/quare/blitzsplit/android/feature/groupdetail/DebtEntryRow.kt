package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.debtSentenceOf
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace

private val debtFaceSize = 32.dp

/*
 * Why: a pair of Saldos: both faces, "Você deve a Bruno", the amount and "Acertar", "Recebi" or "Já pagou"; the whole
 * row and the button open Registrar pagamento for it.
 */
@Composable
internal fun DebtEntryRow(
    debt: DebtRow,
    shape: Shape,
    money: MoneyFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy((-10).dp),
            ) {
                DebtFaceBadge(debt.fromFace)
                DebtFaceBadge(debt.toFace)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = debtSentenceOf(debt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (debt.kind) {
                        DebtKind.YOU_OWE -> money.toSignedText(-debt.amountMinor)
                        DebtKind.OWES_YOU -> money.toSignedText(debt.amountMinor)
                        DebtKind.BETWEEN_OTHERS -> money.toText(debt.amountMinor)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when (debt.kind) {
                        DebtKind.YOU_OWE -> MaterialTheme.colorScheme.error
                        DebtKind.OWES_YOU -> MaterialTheme.colorScheme.primary
                        DebtKind.BETWEEN_OTHERS -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            DebtButton(
                kind = debt.kind,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun DebtButton(
    kind: DebtKind,
    onClick: () -> Unit,
) {
    when (kind) {
        DebtKind.YOU_OWE -> Button(onClick = onClick) { Text(stringResource(R.string.group_detail_balances_settle)) }

        DebtKind.OWES_YOU -> FilledTonalButton(onClick = onClick) {
            Text(stringResource(R.string.group_detail_balances_got_paid))
        }

        DebtKind.BETWEEN_OTHERS -> OutlinedButton(onClick = onClick) {
            Text(stringResource(R.string.group_detail_balances_already_paid))
        }
    }
}

// Why: ringed so the second face's edge reads over the first; a blank avatar for someone no longer in the group.
@Composable
private fun DebtFaceBadge(face: GroupFace?) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
            .padding(2.dp),
    ) {
        if (face == null) {
            Box(
                modifier = Modifier
                    .size(debtFaceSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            )
        } else {
            AvatarBadge(
                initial = face.initials,
                color = face.color,
                size = debtFaceSize,
            )
        }
    }
}
