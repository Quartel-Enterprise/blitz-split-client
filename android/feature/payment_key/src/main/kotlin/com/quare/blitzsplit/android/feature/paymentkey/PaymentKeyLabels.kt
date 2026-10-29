package com.quare.blitzsplit.android.feature.paymentkey

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiState

@get:StringRes
internal val PaymentKeyUiState.titleRes: Int
    get() = when (method) {
        PaymentMethod.PIX -> if (isEditing) R.string.payment_key_title_pix_edit else R.string.payment_key_title_pix_new

        PaymentMethod.BITCOIN -> if (isEditing) {
            R.string.payment_key_title_bitcoin_edit
        } else {
            R.string.payment_key_title_bitcoin_new
        }
    }

@get:StringRes
internal val PaymentKeyType.optionRes: Int
    get() = when (this) {
        PaymentKeyType.CPF -> R.string.payment_key_type_cpf
        PaymentKeyType.PHONE -> R.string.payment_key_type_phone
        PaymentKeyType.EMAIL -> R.string.payment_key_type_email
        PaymentKeyType.RANDOM -> R.string.payment_key_type_random
        PaymentKeyType.LIGHTNING -> R.string.payment_key_network_lightning
        PaymentKeyType.ONCHAIN -> R.string.payment_key_network_onchain
    }

@get:StringRes
internal val PaymentKeyType.fieldLabelRes: Int
    get() = when (this) {
        PaymentKeyType.CPF -> R.string.payment_key_label_cpf
        PaymentKeyType.PHONE -> R.string.payment_key_label_phone
        PaymentKeyType.EMAIL -> R.string.payment_key_label_email
        PaymentKeyType.RANDOM -> R.string.payment_key_label_random
        PaymentKeyType.LIGHTNING -> R.string.payment_key_label_lightning
        PaymentKeyType.ONCHAIN -> R.string.payment_key_label_onchain
    }

@get:StringRes
internal val PaymentKeyType.hintRes: Int
    get() = when (this) {
        PaymentKeyType.CPF -> R.string.payment_key_hint_cpf
        PaymentKeyType.PHONE -> R.string.payment_key_hint_phone
        PaymentKeyType.EMAIL -> R.string.payment_key_hint_email
        PaymentKeyType.RANDOM -> R.string.payment_key_hint_random
        PaymentKeyType.LIGHTNING -> R.string.payment_key_hint_lightning
        PaymentKeyType.ONCHAIN -> R.string.payment_key_hint_onchain
    }

@get:StringRes
internal val PaymentKeyType.defaultTypeRes: Int
    get() = when (this) {
        PaymentKeyType.CPF -> R.string.payment_key_default_type_cpf
        PaymentKeyType.PHONE -> R.string.payment_key_default_type_phone
        PaymentKeyType.EMAIL -> R.string.payment_key_default_type_email
        PaymentKeyType.RANDOM -> R.string.payment_key_default_type_random
        PaymentKeyType.LIGHTNING -> R.string.payment_key_default_type_lightning
        PaymentKeyType.ONCHAIN -> R.string.payment_key_default_type_onchain
    }

internal val PaymentKeyType.keyboardOptions: KeyboardOptions
    get() = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = when (this) {
            PaymentKeyType.CPF -> KeyboardType.Number
            PaymentKeyType.PHONE -> KeyboardType.Phone
            PaymentKeyType.EMAIL -> KeyboardType.Email
            PaymentKeyType.RANDOM, PaymentKeyType.LIGHTNING, PaymentKeyType.ONCHAIN -> KeyboardType.Ascii
        },
    )

@get:StringRes
internal val PaymentKeyFormat.validRes: Int
    get() = when (this) {
        PaymentKeyFormat.CPF -> R.string.payment_key_valid_cpf
        PaymentKeyFormat.PHONE -> R.string.payment_key_valid_phone
        PaymentKeyFormat.EMAIL -> R.string.payment_key_valid_email
        PaymentKeyFormat.RANDOM -> R.string.payment_key_valid_random
        PaymentKeyFormat.LIGHTNING_ADDRESS -> R.string.payment_key_valid_lightning_address
        PaymentKeyFormat.LNURL -> R.string.payment_key_valid_lnurl
        PaymentKeyFormat.SEGWIT -> R.string.payment_key_valid_segwit
        PaymentKeyFormat.TAPROOT -> R.string.payment_key_valid_taproot
    }

@get:StringRes
internal val PaymentKeyError.messageRes: Int
    get() = when (this) {
        PaymentKeyError.INVALID_CPF -> R.string.payment_key_error_invalid_cpf
        PaymentKeyError.INVALID_PHONE -> R.string.payment_key_error_invalid_phone
        PaymentKeyError.INVALID_EMAIL -> R.string.payment_key_error_invalid_email
        PaymentKeyError.INVALID_RANDOM -> R.string.payment_key_error_invalid_random
        PaymentKeyError.INVALID_LIGHTNING -> R.string.payment_key_error_invalid_lightning
        PaymentKeyError.LIGHTNING_INVOICE -> R.string.payment_key_error_lightning_invoice
        PaymentKeyError.INVALID_ONCHAIN -> R.string.payment_key_error_invalid_onchain
        PaymentKeyError.TESTNET_ADDRESS -> R.string.payment_key_error_testnet_address
        PaymentKeyError.ALREADY_SAVED -> R.string.payment_key_error_already_saved
    }
