package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.GuestMergeSheet

/*
 * Why: "Juntar Diego com uma conta" (11.2h): the personal link the server makes as the sheet opens, to copy or share;
 * asking someone who already uses the app comes later ("Em breve").
 */
@Composable
internal fun MergeGuestSheet(
    contact: Contact,
    sheet: GuestMergeSheet,
    isOnline: Boolean,
    onEvent: (ContactProfileUiEvent) -> Unit,
) {
    val money = rememberMoneyFormatter(contact.net.currency)
    ProfileSheet(
        title = stringResource(R.string.contacts_merge_title, contact.name),
        onDismiss = { onEvent(ContactProfileUiEvent.OnMergeDismissed) },
    ) {
        Text(
            text = stringResource(
                R.string.contacts_merge_body,
                contact.name,
                money.toSignedText(contact.net.amountMinor),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.contacts_merge_ways, contact.name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.selectableGroup()) {
            MergeOption(
                title = stringResource(R.string.contacts_merge_personal),
                body = stringResource(R.string.contacts_merge_personal_body, contact.name),
                isSelected = true,
                index = 0,
                onClick = {},
            )
            MergeOption(
                title = stringResource(R.string.contacts_merge_ask),
                body = stringResource(R.string.contacts_merge_ask_body),
                isSelected = false,
                index = 1,
                onClick = { onEvent(ContactProfileUiEvent.OnAskAppUserClicked) },
            )
        }
        MergeLinkBox(
            sheet = sheet,
            isOnline = isOnline,
            onRetry = { onEvent(ContactProfileUiEvent.OnMergeRetryClicked) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val hasLink = sheet.displayLink != null
            OutlinedButton(
                onClick = { onEvent(ContactProfileUiEvent.OnMergeCopyClicked) },
                enabled = hasLink,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.contacts_copy_link))
            }
            Button(
                onClick = { onEvent(ContactProfileUiEvent.OnMergeShareClicked) },
                enabled = hasLink,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    text = stringResource(R.string.contacts_share),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
    }
}

@Composable
private fun MergeOption(
    title: String,
    body: String,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = groupedRowShapeOf(index, 2),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 14.dp,
                bottom = 14.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = null,
            )
        }
    }
}

// Why: the personal link once the server made it; on its way, failed with a retry, or waiting for the internet.
@Composable
private fun MergeLinkBox(
    sheet: GuestMergeSheet,
    isOnline: Boolean,
    onRetry: () -> Unit,
) {
    val link = sheet.displayLink
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                link != null -> Text(
                    text = link,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )

                sheet.isRequesting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.contacts_merge_requesting),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                sheet.hasFailed -> {
                    Text(
                        text = stringResource(R.string.contacts_merge_failed),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.contacts_merge_retry))
                    }
                }

                !isOnline -> Text(
                    text = stringResource(R.string.contacts_merge_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
