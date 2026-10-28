package com.quare.blitzsplit.feature.paymentkey.presentation.model

import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod

data class PaymentKeyUiState(
    val method: PaymentMethod,
    // Why: "Editar chave" with "Excluir chave" for a saved key, "Nova chave" otherwise.
    val isEditing: Boolean,
    // Why: a saved key fills the form once read, so the form doesn't flash empty first.
    val isLoaded: Boolean,
    val type: PaymentKeyType,
    // Why: already masked for CPF and phone ("123.456.789-09", "+55 (11) 91234-5678"), so the UIs show it as is.
    val input: String,
    // Why: what the valid key turned out to be ("Taproot (bc1p)"), null while it isn't valid.
    val format: PaymentKeyFormat?,
    // Why: only once there is something to judge: a paste, the field left, every digit typed, or a key never valid.
    val error: PaymentKeyError?,
    // Why: a pasted bc1… on Lightning, or a Lightning Address on on-chain, switched the network, and the UIs say so.
    val hasSwitchedNetwork: Boolean,
    val isDefault: Boolean,
    // Why: "Hoje: celular", the type of the method's default when it is another key, null otherwise.
    val currentDefaultType: PaymentKeyType?,
    // Why: the first key of a method is its default, and a default only gives way to another key made default.
    val isDefaultLocked: Boolean,
    val isSaving: Boolean,
    val isDeleteConfirmationShown: Boolean,
) {
    val types: List<PaymentKeyType>
        get() = PaymentKeyType.of(method)

    val canSave: Boolean
        get() = isLoaded && format != null && !isSaving

    // Why: "Quem for te pagar vê o CPF completo", unless the CPF is wrong and the error takes its place.
    val showsCpfNotice: Boolean
        get() = type == PaymentKeyType.CPF && error == null
}
