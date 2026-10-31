package com.quare.blitzsplit.android.designsystem.format

import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R

private const val MAX_NAMED_PAYERS = 2

// Why: "Diego e Elisa", joined the way the UI's language lists names.
@Composable
fun nameListOf(names: List<String>): String =
    ListFormatter.getInstance(LocalLocale.current.platformLocale).format(names)

// Why: "Ana e Bruno" for two payers, "3 pessoas" for more, in the order the shared code gives them.
@Composable
fun payerNamesTextOf(names: List<String>): String = if (names.size <= MAX_NAMED_PAYERS) {
    nameListOf(names)
} else {
    pluralStringResource(R.plurals.designsystem_payers_count, names.size, names.size)
}

/*
 * Why: "Ana e Bruno pagaram", "3 pessoas pagaram", or "Ana pagou" once the other payers of a new bill left the group
 * and only one is left paying part of it.
 */
@Composable
fun paidBySeveralOf(names: List<String>): String = names.singleOrNull()?.let {
    stringResource(R.string.designsystem_paid_by_one, it)
} ?: stringResource(R.string.designsystem_paid_by_several, payerNamesTextOf(names))
