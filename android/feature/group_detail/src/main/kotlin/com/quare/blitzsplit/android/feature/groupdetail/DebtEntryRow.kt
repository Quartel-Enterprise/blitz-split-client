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
import androidx.compose.material3.Icon
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
import com.quare.blitzsplit.android.designsystem.component.debtColorOf
import com.quare.blitzsplit.android.designsystem.component.simplifyDebtsIcon
import com.quare.blitzsplit.android.designsystem.format.debtAmountTextOf
import com.quare.blitzsplit.android.designsystem.format.debtSentenceOf
import com.quare.blitzsplit.android.designsystem.layout.TrailingOrBelowLayout
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace

private val debtFaceSize = 32.dp

/*
 * Why: a pair of Saldos: both faces, "Você deve a Bruno", the amount and "Acertar", "Recebi" or "Já pagou"; the whole
 * row and the button open Registrar pagamento for it. A pair "Simplificar dívidas" made carries its icon (13.4).
 */
@Composable
internal fun DebtEntryRow(
    debt: DebtRow,
    shape: Shape,
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
            // Why: in a narrow pane the button goes under the amount, so the sentence keeps its width.
            TrailingOrBelowLayout(
                main = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = debtSentenceOf(debt),
                                modifier = Modifier.weight(
                                    weight = 1f,
                                    fill = false,
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (debt.isSimplified) {
                                Icon(
                                    imageVector = simplifyDebtsIcon(),
                                    contentDescription = stringResource(R.string.group_detail_simplified_pair),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = debtAmountTextOf(debt),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = debtColorOf(debt.kind),
                        )
                    }
                },
                trailing = {
                    DebtButton(
                        kind = debt.kind,
                        onClick = onClick,
                    )
                },
                modifier = Modifier.weight(1f),
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
                photoUrl = face.photoUrl,
                size = debtFaceSize,
            )
        }
    }
}
