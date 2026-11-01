package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quare.blitzsplit.android.designsystem.component.amountColorOf
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow
import com.quare.blitzsplit.feature.contacts.presentation.model.NameMatch

// Why: "Viagem Floripa" for one group, "3 grupos em comum" for more.
@Composable
internal fun groupsTextOf(
    groupCount: Int,
    firstGroupName: String,
): String = if (groupCount > 1) {
    pluralStringResource(R.plurals.contacts_groups_in_common, groupCount, groupCount)
} else {
    firstGroupName
}

// Why: a row's line under the name, "· convidado sem app" for a guest.
@Composable
internal fun rowMetaOf(row: ContactRow): String {
    val groups = groupsTextOf(row.groupCount, row.firstGroupName)
    return if (row.isGuest) stringResource(R.string.contacts_guest_meta, groups) else groups
}

// Why: the word under a net: "te deve", "você deve" or "quitado".
@Composable
internal fun netWordOf(amountMinor: Long): String = stringResource(
    when {
        amountMinor > 0 -> R.string.contacts_owes_you
        amountMinor < 0 -> R.string.contacts_you_owe
        else -> R.string.contacts_settled
    },
)

// Why: what you owe in the error color, what you get back in the primary one, zero in the quiet one.
@Composable
internal fun netColorOf(amountMinor: Long): Color = if (amountMinor == 0L) {
    MaterialTheme.colorScheme.onSurfaceVariant
} else {
    amountColorOf(amountMinor)
}

// Why: where the search matched the name, marked in the tertiary container as the home's search marks it.
@Composable
internal fun highlightedNameOf(
    name: String,
    match: NameMatch?,
): AnnotatedString {
    if (match == null || match.start == match.end || match.end > name.length) return AnnotatedString(name)
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.tertiaryContainer,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    return buildAnnotatedString {
        append(name.substring(0, match.start))
        withStyle(style) { append(name.substring(match.start, match.end)) }
        append(name.substring(match.end))
    }
}
