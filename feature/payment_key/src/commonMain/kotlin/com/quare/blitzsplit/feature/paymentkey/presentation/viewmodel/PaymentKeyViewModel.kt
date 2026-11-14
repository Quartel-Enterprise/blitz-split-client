package com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.DeletePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.DetectPaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.FormatPaymentKeyInput
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.SavePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.ValidatePaymentKey
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
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
    trackEvent: TrackEvent,
) : TrackedViewModel<PaymentKeyUiEvent>(trackEvent) {
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
                isShowingHeldKey = false,
            ),
        )

    val stateWatcher: FlowWatcher<PaymentKeyUiState> = FlowWatcher(uiState)

    private var keys: List<PaymentKey> = emptyList()

    /*
     * Why: the key the form saves to. It starts as the key opened (null for a new one), and a tab of a type the
     * person already holds (one CPF/CNPJ, phone and email each) points it at that saved key instead, so the form
     * edits it rather than adding a second one.
     */
    private var keyId: String? = route.keyId

    // Why: the user's other keys, so the same key can't be saved twice, and whose default this one would replace.
    private var otherKeys: List<PaymentKey> = emptyList()

    // Why: the default switch as the person left it for each key the tabs point at (null for a new key).
    private val defaultChoices = mutableMapOf<String?, Boolean>()

    // Why: a new key opens on a type the person doesn't hold yet, until they pick or type something themselves.
    private var hasChosenType = false
    private var validValue: String? = null

    // Why: each type keeps what was typed in it, so picking another type never carries a phone into CPF or email.
    private val drafts = mutableMapOf<PaymentKeyType, Draft>()
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

    override fun handleEvent(event: PaymentKeyUiEvent) {
        when (event) {
            PaymentKeyUiEvent.OnBackClicked -> leave()

            is PaymentKeyUiEvent.OnTypeSelected -> selectType(event.type)

            is PaymentKeyUiEvent.OnInputChanged -> changeInput(event.input, isPaste = false)

            // Why: an empty clipboard, or one the browser won't read, still counts the tap but keeps the field.
            is PaymentKeyUiEvent.OnPasted -> if (event.text.isNotEmpty()) changeInput(event.text, isPaste = true)

            PaymentKeyUiEvent.OnInputFocusLost -> revealError()

            is PaymentKeyUiEvent.OnDefaultChanged -> changeDefault(event.isDefault)

            PaymentKeyUiEvent.OnSaveClicked -> {
                trackEvent("payment_key_save_clicked", emptyMap())
                save()
            }

            PaymentKeyUiEvent.OnDeleteClicked -> if (keyId != null && !uiState.value.isSaving) {
                trackEvent("payment_key_delete_clicked", emptyMap())
                showDeleteConfirmation(true)
            }

            PaymentKeyUiEvent.OnDeleteDismissed -> if (uiState.value.isDeleteConfirmationShown) {
                trackEvent("payment_key_delete_cancelled", emptyMap())
                showDeleteConfirmation(false)
            }

            PaymentKeyUiEvent.OnDeleteConfirmed -> delete()
        }
    }

    /*
     * Why: a key deleted on another device while it is open here closes the screen. The form is filled once,
     * so a sync landing while someone types never overwrites what they typed.
     */
    private fun updateKeys(keys: List<PaymentKey>) {
        // Why: once saving or deleting, the screen is on its way out, and the key it just wrote would read as saved.
        if (uiState.value.isSaving) return
        val opened = route.keyId?.let { id -> keys.find { it.id == id } }
        if (route.keyId != null && opened == null) {
            leave()
            return
        }
        /*
         * Why: a held key shown in its tab and deleted (or moved to another type) on another device leaves the field,
         * never moving its value onto the key opened, which would then save it in that key's place.
         */
        val shown = keyId
        val shownType = uiState.value.type
        if (shown != null && shown != route.keyId && keys.none { it.id == shown && it.type == shownType }) {
            drafts.remove(uiState.value.type)
            isErrorRevealed = false
            uiState.update { it.copy(input = "") }
        }
        this.keys = keys
        val state = uiState.value
        if (!state.isLoaded && opened != null) {
            // Why: the saved key replaces whatever a type picked before it loaded held, so no draft outlives it.
            drafts.clear()
            isErrorRevealed = true
            uiState.update {
                it.copy(
                    type = opened.type,
                    input = formatPaymentKey(opened),
                )
            }
        } else if (!state.isLoaded && state.input.isBlank()) {
            val held = heldKeyOf(state.type)
            if (!hasChosenType) {
                val free = state.types.firstOrNull { type -> heldKeyOf(type) == null } ?: state.type
                uiState.update { it.copy(type = free) }
            } else if (held != null) {
                // Why: a held type picked before the keys loaded shows its saved key once they do, never a blank edit.
                isErrorRevealed = true
                uiState.update { it.copy(input = formatPaymentKey(held)) }
            }
        }
        retarget()
        validate()
    }

    // Why: the saved key of a one-per-person type, other than the key opened (old duplicates keep their own form).
    private fun heldKeyOf(type: PaymentKeyType): PaymentKey? {
        if (!type.isOnePerPerson) return null
        return keys.find { it.type == type && it.id != route.keyId }
    }

    private fun keyIdFor(type: PaymentKeyType): String? {
        val opened = route.keyId?.let { id -> keys.find { it.id == id } }
        if (opened?.type == type) return route.keyId
        return heldKeyOf(type)?.id ?: route.keyId
    }

    /*
     * Why: points the form at the key its type's tab edits, and works out the default switch against the others.
     * Each key keeps the switch as the person left it, so a visit to a held key's tab and back never undoes it;
     * a key not touched yet shows its own saved default.
     */
    private fun retarget() {
        val state = uiState.value
        val previous = keyId
        if (state.isLoaded && !state.isDefaultLocked) defaultChoices[previous] = state.isDefault
        keyId = keyIdFor(state.type)
        val target = keyId?.let { id -> keys.find { it.id == id } }
        otherKeys = keys.filter { it.id != keyId }
        val ofMethod = otherKeys.filter { it.type.method == route.method }
        val isLocked = ofMethod.isEmpty() || target?.isDefault == true
        val isDefault = when {
            isLocked -> true
            keyId in defaultChoices -> defaultChoices.getValue(keyId)
            else -> target?.isDefault == true
        }
        uiState.update {
            it.copy(
                isLoaded = true,
                isEditing = keyId != null,
                isShowingHeldKey = keyId != null && keyId != route.keyId,
                isDefault = isDefault,
                currentDefaultType = ofMethod
                    .find(
                        PaymentKey::isDefault,
                    )?.type
                    ?.takeUnless { target?.isDefault == true },
                isDefaultLocked = isLocked,
            )
        }
    }

    private fun selectType(type: PaymentKeyType) {
        if (type.method != route.method || type == uiState.value.type) return
        trackEvent("payment_key_type_selected", mapOf("type" to type))
        hasChosenType = true
        drafts[uiState.value.type] = Draft(uiState.value.input, isErrorRevealed, keyId)
        val target = keyIdFor(type)
        // Why: a draft left for another key (a held key deleted or synced in since) never comes back onto this one.
        val draft = drafts[type]?.takeIf { it.keyId == target }
        // Why: only when the tab edits the held key; an old duplicate's own tab keeps the key opened.
        val held = heldKeyOf(type)?.takeIf { it.id == target }
        val input = if (draft == null && held != null) {
            // Why: a type already held shows the key saved in it, ready to edit, instead of a second blank one.
            isErrorRevealed = true
            formatPaymentKey(held)
        } else {
            val formatted = formatPaymentKeyInput(type, draft?.input.orEmpty())
            // Why: a type's own draft comes back as it was left, its error shown again only if it was showing then.
            isErrorRevealed = formatted.text.isNotBlank() && (draft?.isErrorRevealed == true || formatted.isComplete)
            formatted.text
        }
        uiState.update {
            it.copy(
                type = type,
                input = input,
                hasSwitchedNetwork = false,
            )
        }
        if (uiState.value.isLoaded) retarget()
        validate()
    }

    /*
     * Why: a paste of another type's key switches to that type ("Ao colar, o tipo é detectado sozinho"), but a
     * key valid for the type picked stays there: 11 digits can be a CPF or a phone. A random key typed by hand
     * stays random, since its first digits can pass for a CPF or a phone and the mask would cut the rest.
     */
    private fun changeInput(
        raw: String,
        isPaste: Boolean,
    ) {
        val state = uiState.value
        val detected = detectPaymentKeyType(route.method, raw)
            ?.takeIf {
                (isPaste || state.type != PaymentKeyType.RANDOM) && it != state.type && validatePaymentKey(
                    state.type,
                    raw,
                    otherKeys,
                ) !is PaymentKeyValidation.Valid
            }
        val type = detected ?: state.type
        if (raw.isNotBlank()) hasChosenType = true
        val input =
            formatPaymentKeyInput(type, toInputAfterErase(state.input, raw, type, isSwitched = detected != null))
        // Why: what was typed moved to the type detected, while a paste replaced the field, so what it held stays.
        if (detected != null) {
            if (isPaste) drafts[state.type] = Draft(state.input, isErrorRevealed, keyId) else drafts.remove(state.type)
            isErrorRevealed = false
        }
        if (isPaste || input.isComplete) isErrorRevealed = true
        if (input.text.isBlank()) isErrorRevealed = false
        uiState.update {
            it.copy(
                type = type,
                input = input.text,
                // Why: the note names what the key is, so only a key valid as that type earns it.
                hasSwitchedNetwork = if (detected != null) {
                    route.method == PaymentMethod.BITCOIN &&
                        validatePaymentKey(type, raw, otherKeys) is PaymentKeyValidation.Valid
                } else {
                    it.hasSwitchedNetwork
                },
            )
        }
        // Why: a key typed or pasted of a type already held goes to that saved key, never to a second one.
        if (detected != null && state.isLoaded) retarget()
        validate()
    }

    /*
     * Why: backspace over a character the mask added ("123.") leaves the same digits, which the mask would put
     * right back, so it takes the digit before it instead. The phone's "+55" is the mask's too, so erasing up to
     * it changes nothing, unless no typed digit is left: then the field empties. Only ASCII digits count, as in
     * the mask.
     */
    private fun toInputAfterErase(
        previous: String,
        raw: String,
        type: PaymentKeyType,
        isSwitched: Boolean,
    ): String {
        val isMasked = type == PaymentKeyType.CPF || type == PaymentKeyType.PHONE
        val digits = previous.filter { it.isAsciiDigit() }
        val erasedMaskOnly = raw.length < previous.length && raw.filter { it.isAsciiDigit() } == digits
        if (!isMasked || isSwitched || !erasedMaskOnly) return raw
        val erasedAt = previous.indices.first { it >= raw.length || previous[it] != raw[it] }
        val digitsBefore = previous.take(erasedAt).count { it.isAsciiDigit() }
        val maskDigits = if (type == PaymentKeyType.PHONE && previous.trim().startsWith("+")) 2 else 0
        if (digitsBefore <= maskDigits) return if (digits.length > maskDigits) previous else ""
        val remaining = digits.removeRange(digitsBefore - 1, digitsBefore)
        return if (maskDigits > 0) "+$remaining" else remaining
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

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
        if (uiState.value.isDefaultLocked || isDefault == uiState.value.isDefault) return
        trackEvent("payment_key_default_toggled", mapOf("is_default" to isDefault))
        uiState.update { it.copy(isDefault = isDefault) }
    }

    private fun save() {
        val state = uiState.value
        val value = validValue
        if (!state.canSave || value == null) return
        val savedKeyId = keyId
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            savePaymentKey(
                keyId = savedKeyId,
                type = state.type,
                value = value,
                isDefault = state.isDefault,
            )
            trackEvent(
                "payment_key_saved",
                mapOf("type" to state.type, "is_new" to (savedKeyId == null), "is_default" to state.isDefault),
            )
            leave()
        }
    }

    private fun showDeleteConfirmation(isShown: Boolean) {
        uiState.update { it.copy(isDeleteConfirmationShown = isShown) }
    }

    private fun delete() {
        val id = keyId ?: return
        if (!uiState.value.isDeleteConfirmationShown || uiState.value.isSaving) return
        trackEvent("payment_key_delete_confirmed", emptyMap())
        uiState.update {
            it.copy(
                isSaving = true,
                isDeleteConfirmationShown = false,
            )
        }
        viewModelScope.launch {
            deletePaymentKey(id)
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

    private data class Draft(
        val input: String,
        val isErrorRevealed: Boolean,
        // Why: the key the draft was typed for, so it only comes back while its tab still edits that key.
        val keyId: String?,
    )
}
