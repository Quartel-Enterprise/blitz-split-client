package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.selectableRowColorOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaMatch
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow

private val faceSize = 40.dp

// Why: someone of Contatos, A to Z: the face, the name with their groups, and the net with its word.
@Composable
internal fun ContactListRow(
    row: ContactRow,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(row.net.currency)
    val amountMinor = row.net.amountMinor
    SelectableListRow(
        isSelected = isSelected,
        index = index,
        count = count,
        onClick = onClick,
        modifier = modifier,
    ) {
        AvatarBadge(
            initial = row.initials,
            color = row.color,
            size = faceSize,
        )
        NameColumn(
            name = row.name,
            meta = rowMetaOf(row),
            row = row,
            modifier = Modifier.weight(1f),
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = money.toSignedText(amountMinor),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = netColorOf(amountMinor),
                maxLines = 1,
            )
            Text(
                text = netWordOf(amountMinor),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: a tappable row of Contatos' list, grouped with its neighbours, tinted while what it opens shows in the pane.
@Composable
private fun SelectableListRow(
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = groupedRowShapeOf(index, count),
        color = selectableRowColorOf(isSelected),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun NameColumn(
    name: String,
    meta: String,
    row: ContactRow?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = highlightedNameOf(name, row?.match),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Why: someone of the agenda, with "Convidar", in Contatos' search and in "Convidar da agenda".
@Composable
internal fun AgendaContactRow(
    match: AgendaMatch,
    index: Int,
    count: Int,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contact = match.contact
    val inviteLabel = stringResource(R.string.contacts_invite_person, contact.name)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = groupedRowShapeOf(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(faceSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = match.initials,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = highlightedNameOf(contact.name, match.match),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                contact.phone?.let { phone ->
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            FilledTonalButton(
                onClick = onInvite,
                modifier = Modifier.semantics { contentDescription = inviteLabel },
            ) {
                Text(stringResource(R.string.contacts_invite))
            }
        }
    }
}

// Why: "Convidar da agenda" and "Convidar com link ou QR", an icon in its tile, the title and what it does.
@Composable
internal fun InviteActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableListRow(
        isSelected = isSelected,
        index = index,
        count = count,
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(faceSize)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        NameColumn(
            name = title,
            meta = subtitle,
            row = null,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: "Pessoas dos seus grupos · 7", "Chamar mais gente", "Nos seus grupos · 1" over their rows.
@Composable
internal fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 8.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
