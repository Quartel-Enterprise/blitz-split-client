package com.quare.blitzsplit.android.designsystem.format

import androidx.annotation.StringRes
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod

// Why: "Como foi pago" in Registrar pagamento and the payment's line in Despesas name the method alike.
val SettlementMethod.labelRes: Int
    @StringRes get() = when (this) {
        SettlementMethod.PIX -> R.string.designsystem_method_pix
        SettlementMethod.CASH -> R.string.designsystem_method_cash
        SettlementMethod.TRANSFER -> R.string.designsystem_method_transfer
        SettlementMethod.OTHER -> R.string.designsystem_method_other
    }
