package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.simplifyDebtsIcon
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: "Dívidas simplificadas" over the pairs (13.4), since someone may now pay or receive from whoever they never split
 * a bill with; "Como funciona" explains it.
 */
@Composable
internal fun SimplificationCard(
    simplification: DebtSimplificationSummary,
    onHowItWorksClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 12.dp,
                top = 14.dp,
                bottom = 2.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = simplifyDebtsIcon(),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(22.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(DesignR.string.designsystem_simplified_debts),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (simplification.isAlreadyMinimal) {
                        stringResource(R.string.group_detail_simplified_minimal)
                    } else {
                        pluralStringResource(
                            R.plurals.group_detail_simplified_text,
                            simplification.paymentCount,
                            simplification.paymentCount,
                            simplification.unsimplifiedPaymentCount,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onHowItWorksClick,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.group_detail_simplified_how),
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}
