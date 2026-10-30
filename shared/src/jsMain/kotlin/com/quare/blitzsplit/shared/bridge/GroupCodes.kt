package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

// Why: "Novo grupo" and "Editar grupo" hand the web the type and currency choices as codes, in the order they're drawn.
internal fun List<GroupType>.toCodes(): Array<String> = map(GroupType::code).toTypedArray()

internal fun List<GroupCurrency>.toCurrencyCodes(): Array<String> = map(GroupCurrency::code).toTypedArray()
