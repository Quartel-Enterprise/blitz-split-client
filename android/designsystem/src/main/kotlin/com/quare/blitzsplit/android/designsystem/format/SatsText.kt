package com.quare.blitzsplit.android.designsystem.format

// Why: bitcoin in satoshis as the UI's language writes it: the amount's pattern ("%1$s sats") and a field's symbol.
data class SatsText(
    val amount: String,
    val symbol: String,
)
