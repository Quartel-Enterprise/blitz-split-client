package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance

/*
 * Why: "Por grupo", what each group adds to the net with someone, then the net: "Líquido, já compensado" over several
 * groups, "Total" for one. Saldos opens it inside a row, and Registrar pagamento shows it when settling up across groups.
 */
@Composable
fun PersonGroupsCard(
    groups: List<PersonGroupBalance>,
    netMinor: Long,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
        ) {
            groups.forEach { part ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GroupBadge(
                        initials = part.groupInitials,
                        color = part.groupColor,
                        size = 28.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = part.groupName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(
                                if (part.amountMinor <
                                    0
                                ) {
                                    R.string.designsystem_person_you_owe
                                } else {
                                    R.string.designsystem_person_owes_you
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = money.toSignedText(part.amountMinor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColorOf(part.amountMinor),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                modifier = Modifier.padding(
                    top = 10.dp,
                    bottom = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (groups.size > 1) R.string.designsystem_person_net else R.string.designsystem_person_total,
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = money.toSignedText(netMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColorOf(netMinor),
                )
            }
        }
    }
}

// Why: what you owe in the error color, what you get back in the primary one, as the home's rows.
@Composable
fun amountColorOf(amountMinor: Long): Color =
    if (amountMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
