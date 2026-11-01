package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.EmptyGroupsImage

/*
 * Why: "Nenhum grupo ainda", in a row on a phone in landscape and larger on a tablet; in a book's half the
 * large card keeps the canvas' tighter padding and stacks its two actions, which don't fit side by side there.
 * Grupos and Início's first use each say it with their own body.
 */
@Composable
internal fun EmptyGroupsCard(
    @StringRes body: Int,
    onCreateGroup: () -> Unit,
    onJoinWithLink: () -> Unit,
    isHorizontal: Boolean,
    isLarge: Boolean,
    modifier: Modifier = Modifier,
    isInBookHalf: Boolean = false,
) {
    val isWide = isLarge && !isInBookHalf
    Surface(
        modifier = modifier
            .then(if (isLarge) Modifier.widthIn(max = 600.dp) else Modifier)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (isHorizontal) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 28.dp,
                    vertical = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmptyGroupsImage(scale = 0.94f)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EmptyGroupsText(
                        body = body,
                        textAlign = TextAlign.Start,
                        isLarge = false,
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CreateGroupButton(onClick = onCreateGroup)
                        JoinWithLinkButton(onClick = onJoinWithLink)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(
                    horizontal = when {
                        isWide -> 40.dp
                        isLarge -> 20.dp
                        else -> 24.dp
                    },
                    vertical = when {
                        isWide -> 40.dp
                        isLarge -> 28.dp
                        else -> 32.dp
                    },
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EmptyGroupsImage(scale = if (isWide) 1.18f else 1f)
                EmptyGroupsText(
                    body = body,
                    textAlign = TextAlign.Center,
                    isLarge = isLarge,
                )
                if (isWide) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CreateGroupButton(onClick = onCreateGroup)
                        JoinWithLinkButton(onClick = onJoinWithLink)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CreateGroupButton(
                            onClick = onCreateGroup,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        JoinWithLinkButton(
                            onClick = onJoinWithLink,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGroupsText(
    @StringRes body: Int,
    textAlign: TextAlign,
    isLarge: Boolean,
) {
    Text(
        text = stringResource(R.string.groups_empty_title),
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(body),
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CreateGroupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.groups_create),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun JoinWithLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCode2,
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.groups_join_with_link),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
