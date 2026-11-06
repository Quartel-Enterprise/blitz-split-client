package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: the currency a group with money in more than one picks its balances and figures in; one chip per currency, labeled by its code, drawn only with two or more.
@Composable
fun CurrencyChipRow(
    currencies: List<GroupCurrency>,
    selected: GroupCurrency,
    onSelected: (GroupCurrency) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    if (currencies.size < 2) return
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = currencies,
            key = { it.code },
        ) { currency ->
            FilterChip(
                selected = currency == selected,
                onClick = { onSelected(currency) },
                label = { Text(currency.code) },
            )
        }
    }
}
