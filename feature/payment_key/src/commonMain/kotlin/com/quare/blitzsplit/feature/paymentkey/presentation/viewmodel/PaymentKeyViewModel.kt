package com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.DeletePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.DetectPaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKeyInput
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.SavePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentKeyViewModel(
    private val route: Route.PaymentKey,
    private val navigator: Navigator,
    private val validatePaymentKey: ValidatePaymentKey,
    private val detectPaymentKeyType: DetectPaymentKeyType,
    private val formatPaymentKeyInput: FormatPaymentKeyInput,
    private val formatPaymentKey: FormatPaymentKey,
    private val savePaymentKey: SavePaymentKey,
    private val deletePaymentKey: DeletePaymentKey,
    observePaymentKeys: ObservePaymentKeys,
) : ViewModel() {
    val uiState: StateFlow<PaymentKeyUiState>
        field = MutableStateFlow(
            PaymentKeyUiState(
                method = route.method,
                isEditing = route.keyId != null,
                isLoaded = false,
                type = PaymentKeyType.of(route.method).first(),
                input = "",
                format = null,
                error = null,
                hasSwitchedNetwork = false,
                isDefault = false,
                currentDefaultType = null,
                isDefaultLocked = false,
                isSaving = false,
                isDeleteConfirmationShown = false,
            ),
        )

    val stateWatcher: FlowWatcher<PaymentKeyUiState> = FlowWatcher(uiState)

    // Why: the user's other keys, so the same key can't be saved twice, and whose default this one would replace.
    private var otherKeys: List<PaymentKey> = emptyList()
    private var validValue: String? = null
    private var isErrorRevealed = false

    // Why: these aren't a key half typed but the wrong thing altogether, so they show right away.
    private val alwaysShownErrors = setOf(
        PaymentKeyError.TESTNET_ADDRESS,
        PaymentKeyError.LIGHTNING_INVOICE,
        PaymentKeyError.ALREADY_SAVED,
    )

    init {
        observePaymentKeys()
            .onEach(::updateKeys)
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PaymentKeyUiEvent) {
        when (event) {
            PaymentKeyUiEvent.OnBackClicked -> leave()
            is PaymentKeyUiEvent.OnTypeSelected -> selectType(event.type)
            is PaymentKeyUiEvent.OnInputChanged -> changeInput(event.input, isPaste = false)
            is PaymentKeyUiEvent.OnPasted -> changeInput(event.text, isPaste = true)
            PaymentKeyUiEvent.OnInputFocusLost -> revealError()
            is PaymentKeyUiEvent.OnDefaultChanged -> changeDefault(event.isDefault)
            PaymentKeyUiEvent.OnSaveClicked -> save()
            PaymentKeyUiEvent.OnDeleteClicked -> if (route.keyId != null) showDeleteConfirmation(true)
            PaymentKeyUiEvent.OnDeleteDismissed -> showDeleteConfirmation(false)
            PaymentKeyUiEvent.OnDeleteConfirmed -> delete()
        }
    }

    /*
     * Why: a key deleted on another device while it is open here closes the screen. The form is filled once,
     * so a sync landing while someone types never overwrites what they typed.
     */
    private fun updateKeys(keys: List<PaymentKey>) {
        val edited = route.keyId?.let { id -> keys.find { it.id == id } }
        if (route.keyId != null && edited == null) {
            leave()
            return
        }
        otherKeys = keys.filter { it.id != route.keyId }
        val ofMethod = otherKeys.filter { it.type.method == route.method }
        val state = uiState.value
        if (!state.isLoaded && edited != null) {
            isErrorRevealed = true
            uiState.update {
                it.copy(
                    type = edited.type,
                    input = formatPaymentKey(edited),
                )
            }
        }
        val wasDefault = if (state.isLoaded) state.isDefault else edited?.isDefault == true
        uiState.update {
            it.copy(
                isLoaded = true,
                isDefault = wasDefault || ofMethod.isEmpty(),
                currentDefaultType = ofMethod
                    .find(
                        PaymentKey::isDefault,
                    )?.type
                    ?.takeUnless { edited?.isDefault == true },
                isDefaultLocked = ofMethod.isEmpty() || edited?.isDefault == true,
            )
        }
        validate()
    }

    private fun selectType(type: PaymentKeyType) {
        if (type.method != route.method || type == uiState.value.type) return
        isErrorRevealed = false
        val input = formatPaymentKeyInput(type, uiState.value.input)
        uiState.update {
            it.copy(
                type = type,
                input = input.text,
                hasSwitchedNetwork = false,
            )
        }
        if (input.isComplete) isErrorRevealed = true
        validate()
    }

    /*
     * Why: a paste of another type's key switches to that type ("Ao colar, o tipo é detectado sozinho"), but a
     * key valid for the type picked stays there: 11 digits can be a CPF or a phone.
     */
    private fun changeInput(
        raw: String,
        isPaste: Boolean,
    ) {
        val state = uiState.value
        val detected = detectPaymentKeyType(route.method, raw)
            ?.takeIf {
                it != state.type && validatePaymentKey(
                    state.type,
                    raw,
                    otherKeys,
                ) !is PaymentKeyValidation.Valid
            }
        val type = detected ?: state.type
        val input =
            formatPaymentKeyInput(type, toInputAfterErase(state.input, raw, type, isSwitched = detected != null))
        if (detected != null) isErrorRevealed = false
        if (isPaste || input.isComplete) isErrorRevealed = true
        if (input.text.isBlank()) isErrorRevealed = false
        uiState.update {
            it.copy(
                type = type,
                input = input.text,
                hasSwitchedNetwork = if (detected !=
                    null
                ) {
                    route.method == PaymentMethod.BITCOIN
                } else {
                    it.hasSwitchedNetwork
                },
            )
        }
        validate()
    }

    /*
     * Why: backspace over a character the mask added ("123.") leaves the same digits, which the mask would put
     * right back, so it takes the digit before it instead. The phone's "+55" is the mask's too, so erasing up to
     * it changes nothing.
     */
    private fun toInputAfterErase(
        previous: String,
        raw: String,
        type: PaymentKeyType,
        isSwitched: Boolean,
    ): String {
        val isMasked = type == PaymentKeyType.CPF || type == PaymentKeyType.PHONE
        val digits = previous.filter(Char::isDigit)
        val erasedMaskOnly = raw.length < previous.length && raw.filter(Char::isDigit) == digits
        if (!isMasked || isSwitched || !erasedMaskOnly) return raw
        val erasedAt = previous.indices.first { it >= raw.length || previous[it] != raw[it] }
        val digitsBefore = previous.take(erasedAt).count(Char::isDigit)
        val maskDigits = if (type == PaymentKeyType.PHONE && previous.trim().startsWith("+")) 2 else 0
        if (digitsBefore <= maskDigits) return previous
        val remaining = digits.removeRange(digitsBefore - 1, digitsBefore)
        return if (maskDigits > 0) "+$remaining" else remaining
    }

    private fun revealError() {
        if (uiState.value.input.isBlank()) return
        isErrorRevealed = true
        validate()
    }

    private fun validate() {
        val state = uiState.value
        val validation = state.input
            .takeIf(String::isNotBlank)
            ?.let { input -> validatePaymentKey(state.type, input, otherKeys) }
        val valid = validation as? PaymentKeyValidation.Valid
        val error = (validation as? PaymentKeyValidation.Invalid)?.error
        validValue = valid?.value
        uiState.update {
            it.copy(
                format = valid?.format,
                error = error?.takeIf { isErrorRevealed || it in alwaysShownErrors },
            )
        }
    }

    private fun changeDefault(isDefault: Boolean) {
        if (uiState.value.isDefaultLocked) return
        uiState.update { it.copy(isDefault = isDefault) }
    }

    private fun save() {
        val state = uiState.value
        val value = validValue
        if (!state.canSave || value == null) return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            savePaymentKey(
                keyId = route.keyId,
                type = state.type,
                value = value,
                isDefault = state.isDefault,
            )
            leave()
        }
    }

    private fun showDeleteConfirmation(isShown: Boolean) {
        uiState.update { it.copy(isDeleteConfirmationShown = isShown) }
    }

    private fun delete() {
        val keyId = route.keyId ?: return
        if (!uiState.value.isDeleteConfirmationShown || uiState.value.isSaving) return
        uiState.update {
            it.copy(
                isSaving = true,
                isDeleteConfirmationShown = false,
            )
        }
        viewModelScope.launch {
            deletePaymentKey(keyId)
            leave()
        }
    }

    /*
     * Why: the save and the deletion both close the screen, and so does the key vanishing, but only while it is
     * still on top: a native back (a swipe, the browser's, a dialog dismissed) may have closed it already.
     */
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}
