@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsPaymentKeyState(
    // Why: the PaymentMethod's name, "PIX" or "BITCOIN".
    val method: String,
    val isEditing: Boolean,
    val isLoaded: Boolean,
    // Why: the PaymentKeyType's name; types lists the method's, in the order the UIs draw them.
    val type: String,
    val types: Array<String>,
    val input: String,
    // Why: the PaymentKeyFormat's name, null while the key isn't valid.
    val format: String?,
    // Why: the PaymentKeyError's name, null while there is none to show.
    val error: String?,
    val hasSwitchedNetwork: Boolean,
    val isDefault: Boolean,
    // Why: the PaymentKeyType's name of the method's current default, when it is another key.
    val currentDefaultType: String?,
    val isDefaultLocked: Boolean,
    val isSaving: Boolean,
    val isDeleteConfirmationShown: Boolean,
    val canSave: Boolean,
    val showsCpfNotice: Boolean,
)

class PaymentKeyPresenter(
    route: JsRoute,
) {
    private val paymentKeyRoute = route.route as Route.PaymentKey
    private val holder = ViewModelHolder(PaymentKeyViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(paymentKeyRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsPaymentKeyState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsPaymentKeyState(
                method = state.method.name,
                isEditing = state.isEditing,
                isLoaded = state.isLoaded,
                type = state.type.name,
                types = state.types.map(PaymentKeyType::name).toTypedArray(),
                input = state.input,
                format = state.format?.name,
                error = state.error?.name,
                hasSwitchedNetwork = state.hasSwitchedNetwork,
                isDefault = state.isDefault,
                currentDefaultType = state.currentDefaultType?.name,
                isDefaultLocked = state.isDefaultLocked,
                isSaving = state.isSaving,
                isDeleteConfirmationShown = state.isDeleteConfirmationShown,
                canSave = state.canSave,
                showsCpfNotice = state.showsCpfNotice,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    /*
     * Why: the ViewModel masks the input as it handles the event, so the field reads it back in the same
     * keystroke. Waiting for the state would leave a digit the mask refused on screen, since a state equal to
     * the last one is never sent.
     */
    fun getInput(): String = viewModel.uiState.value.input

    fun back() {
        viewModel.onEvent(PaymentKeyUiEvent.OnBackClicked)
    }

    fun selectType(typeCode: String) {
        val type = PaymentKeyType.entries.find { it.name == typeCode } ?: return
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(type))
    }

    fun changeInput(input: String) {
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(input))
    }

    fun paste(text: String) {
        viewModel.onEvent(PaymentKeyUiEvent.OnPasted(text))
    }

    fun inputFocusLost() {
        viewModel.onEvent(PaymentKeyUiEvent.OnInputFocusLost)
    }

    fun changeDefault(isDefault: Boolean) {
        viewModel.onEvent(PaymentKeyUiEvent.OnDefaultChanged(isDefault))
    }

    fun save() {
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)
    }

    fun delete() {
        viewModel.onEvent(PaymentKeyUiEvent.OnDeleteClicked)
    }

    fun confirmDelete() {
        viewModel.onEvent(PaymentKeyUiEvent.OnDeleteConfirmed)
    }

    fun dismissDelete() {
        viewModel.onEvent(PaymentKeyUiEvent.OnDeleteDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}
