package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.groupCurrencyLabel
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: "Moeda" of Novo grupo and Editar grupo, the cryptocurrencies under their own header after the others.
@Composable
fun CurrencyMenuContent(
    currencies: List<GroupCurrency>,
    selected: GroupCurrency,
    onSelected: (GroupCurrency) -> Unit,
) {
    val (crypto, fiat) = currencies.partition { it.isCrypto }
    fiat.forEach { CurrencyMenuItem(it, isSelected = it == selected, onSelected) }
    if (crypto.isEmpty()) return
    HorizontalDivider()
    Text(
        text = stringResource(R.string.designsystem_currency_crypto_section),
        modifier = Modifier.padding(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    crypto.forEach { CurrencyMenuItem(it, isSelected = it == selected, onSelected) }
}

@Composable
private fun CurrencyMenuItem(
    currency: GroupCurrency,
    isSelected: Boolean,
    onSelected: (GroupCurrency) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(groupCurrencyLabel(currency, LocalLocale.current.platformLocale)) },
        onClick = { onSelected(currency) },
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
    )
}
