package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.model.PaymentKeyType

// Why: one key of whoever receives, in "Pagar Ana" (4.5); each UI names its type ("Celular", "E-mail").
data class ReceiverKey(
    val id: String,
    val type: PaymentKeyType,
    // Why: what "Copiar chave" copies, as the Pix directory and the wallets write it ("+5521987654321").
    val value: String,
    // Why: what the screen shows, whole, since it is what the payer copies: "+55 (21) 98765-4321".
    val text: String,
    val isDefault: Boolean,
)
