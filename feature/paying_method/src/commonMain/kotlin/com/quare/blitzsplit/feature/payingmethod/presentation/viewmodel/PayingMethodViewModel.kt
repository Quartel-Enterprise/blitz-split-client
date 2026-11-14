package com.quare.blitzsplit.feature.payingmethod.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodRules
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.domain.usecase.DeletePayingMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePayingMethods
import com.quare.blitzsplit.core.expenses.domain.usecase.SavePayingMethod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.guardUnsavedInput
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveShowsPix
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiEvent
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: Profile's "New payment method" and "Edit payment method": a type, a name, a card's color and the default. Private
 * like the methods themselves: nothing here ever reaches the person's groups.
 */
class PayingMethodViewModel(
    private val route: Route.PayingMethod,
    private val navigator: Navigator,
    private val savePayingMethod: SavePayingMethod,
    private val deletePayingMethod: DeletePayingMethod,
    observePayingMethods: ObservePayingMethods,
    observeShowsPix: ObserveShowsPix,
    trackEvent: TrackEvent,
) : TrackedViewModel<PayingMethodUiEvent>(trackEvent) {
    val uiState: StateFlow<PayingMethodUiState>
        field = MutableStateFlow(
            PayingMethodUiState(
                isEditing = route.methodId != null,
                isLoaded = false,
                types = PayingMethodType.entries,
                type = PayingMethodType.CREDIT_CARD,
                name = "",
                cardColor = CardColor.PURPLE,
                isDefault = false,
                isSaving = false,
                isDeleteConfirmationShown = false,
                openedInput = null,
            ),
        )

    val stateWatcher: FlowWatcher<PayingMethodUiState> = FlowWatcher(uiState)

    init {
        combine(observePayingMethods(), observeShowsPix(), ::Pair)
            .onEach { (methods, showsPix) -> update(methods, showsPix) }
            .launchIn(viewModelScope)
        navigator.guardUnsavedInput(viewModelScope, route, uiState.map { it.hasUnsavedInput })
    }

    override fun handleEvent(event: PayingMethodUiEvent) {
        when (event) {
            PayingMethodUiEvent.OnBackClicked -> navigator.navigateBackFrom(route)

            is PayingMethodUiEvent.OnTypeSelected -> selectType(event.type)

            // Why: capped here, so every platform's field stops at the same length.
            is PayingMethodUiEvent.OnNameChanged -> uiState.update {
                it.copy(name = event.name.take(PayingMethodRules.MAX_NAME_LENGTH))
            }

            is PayingMethodUiEvent.OnCardColorSelected -> selectColor(event.color)

            is PayingMethodUiEvent.OnDefaultChanged -> changeDefault(event.isDefault)

            PayingMethodUiEvent.OnSaveClicked -> {
                trackEvent("paying_method_save_clicked", emptyMap())
                save()
            }

            PayingMethodUiEvent.OnDeleteClicked -> if (route.methodId != null && !uiState.value.isSaving) {
                trackEvent("paying_method_delete_clicked", emptyMap())
                uiState.update { it.copy(isDeleteConfirmationShown = true) }
            }

            PayingMethodUiEvent.OnDeleteDismissed -> if (uiState.value.isDeleteConfirmationShown) {
                trackEvent("paying_method_delete_cancelled", emptyMap())
                uiState.update { it.copy(isDeleteConfirmationShown = false) }
            }

            PayingMethodUiEvent.OnDeleteConfirmed -> delete()
        }
    }

    /*
     * Why: the form is filled once, so a sync landing while someone types never overwrites it; a method deleted on
     * another device while it is open here closes the screen. The types follow the Pix rule and the cash already held.
     */
    private fun update(
        methods: List<PayingMethod>,
        showsPix: Boolean,
    ) {
        val state = uiState.value
        if (state.isSaving) return
        val opened = route.methodId?.let { id -> methods.find { it.id == id } }
        if (route.methodId != null && opened == null) {
            // Why: nothing typed can be saved to a method that is gone, so leaving asks nothing.
            if (state.isLoaded) leave()
            return
        }
        val types = PayingMethodType.entries.filter { type ->
            when {
                type == opened?.type -> true
                type == PayingMethodType.PIX -> showsPix
                type.isOnePerPerson -> methods.none { it.type == type }
                else -> true
            }
        }
        uiState.update { current ->
            val filled = if (current.isLoaded) {
                current
            } else if (opened != null) {
                current.copy(
                    type = opened.type,
                    name = opened.name,
                    cardColor = opened.cardColor ?: current.cardColor,
                    isDefault = opened.isDefault,
                )
            } else {
                // Why: the person's first method is their default unless they say otherwise.
                current.copy(isDefault = methods.isEmpty())
            }
            // Why: once filled, a sync never changes the type the person is on; saving a second Cash then fails instead.
            val kept = filled.type.takeIf { current.isLoaded }
            val withTypes = filled.copy(
                types = PayingMethodType.entries.filter { it in types || it == kept },
                type = kept ?: filled.type.takeIf { it in types } ?: types.first(),
                isLoaded = true,
            )
            withTypes.copy(openedInput = current.openedInput ?: withTypes.input)
        }
    }

    private fun selectType(type: PayingMethodType) {
        val state = uiState.value
        if (type == state.type || type !in state.types) return
        trackEvent("paying_method_type_selected", mapOf("type" to type.code))
        uiState.update { it.copy(type = type) }
    }

    private fun selectColor(color: CardColor) {
        if (color == uiState.value.cardColor) return
        trackEvent("paying_method_card_color_selected", mapOf("color" to color.code))
        uiState.update { it.copy(cardColor = color) }
    }

    private fun changeDefault(isDefault: Boolean) {
        if (isDefault == uiState.value.isDefault) return
        trackEvent("paying_method_default_toggled", mapOf("is_default" to isDefault))
        uiState.update { it.copy(isDefault = isDefault) }
    }

    private fun save() {
        val state = uiState.value
        if (!state.canSave) return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            savePayingMethod(
                methodId = route.methodId,
                type = state.type,
                name = state.name,
                cardColor = state.cardColor.takeIf { state.type.isCard },
                isDefault = state.isDefault,
            ).onSuccess {
                trackEvent(
                    "paying_method_saved",
                    mapOf(
                        "type" to state.type.code,
                        "is_new" to (route.methodId == null),
                        "is_default" to state.isDefault,
                    ),
                )
                leave()
            }.onFailure {
                uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun delete() {
        val id = route.methodId ?: return
        if (!uiState.value.isDeleteConfirmationShown || uiState.value.isSaving) return
        trackEvent("paying_method_delete_confirmed", emptyMap())
        uiState.update {
            it.copy(
                isSaving = true,
                isDeleteConfirmationShown = false,
            )
        }
        viewModelScope.launch {
            deletePayingMethod(id)
                .onSuccess { leave() }
                .onFailure { uiState.update { it.copy(isSaving = false) } }
        }
    }

    // Why: once saved or deleted, what was typed is no longer at stake, so leaving asks nothing.
    private fun leave() {
        navigator.markUnsaved(route, false)
        navigator.navigateBackFrom(route)
    }
}
