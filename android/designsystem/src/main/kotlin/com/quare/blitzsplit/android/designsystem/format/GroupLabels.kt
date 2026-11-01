package com.quare.blitzsplit.android.designsystem.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import java.util.Currency
import java.util.Locale

// Why: "Novo grupo" and "Editar grupo" name the group's type and currency alike.
val GroupType.labelRes: Int
    @StringRes get() = when (this) {
        GroupType.TRIP -> R.string.designsystem_group_type_trip
        GroupType.HOME -> R.string.designsystem_group_type_home
        GroupType.EVENT -> R.string.designsystem_group_type_event
        GroupType.OTHER -> R.string.designsystem_group_type_other
    }

// Why: "Divisão padrão" and Dividir's notice name the group's default way to split alike.
val DefaultSplitMode.labelRes: Int
    @StringRes get() = when (this) {
        DefaultSplitMode.EQUAL -> R.string.designsystem_default_split_equal
        DefaultSplitMode.SHARES -> R.string.designsystem_default_split_shares
        DefaultSplitMode.PERCENT -> R.string.designsystem_default_split_percent
        DefaultSplitMode.INCOME -> R.string.designsystem_default_split_income
    }

// Why: "Real brasileiro (R$)", named by the platform in the device's language.
@Composable
fun groupCurrencyLabel(
    currency: GroupCurrency,
    locale: Locale,
): String {
    val label = remember(currency, locale) {
        val platform = Currency.getInstance(currency.code)
        platform.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) } to platform.getSymbol(locale)
    }
    return stringResource(R.string.designsystem_currency_value, label.first, label.second)
}
