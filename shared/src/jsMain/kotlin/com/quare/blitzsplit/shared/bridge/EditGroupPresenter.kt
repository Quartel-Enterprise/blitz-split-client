@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsEditGroupState(
    val name: String,
    val initials: String,
    val color: String,
    // Why: the GroupType's code ("trip"); types lists them in the order the UIs draw them.
    val type: String,
    val types: Array<String>,
    // Why: the ISO 4217 code ("BRL"); each UI names it in its own language.
    val currency: String,
    val currencies: Array<String>,
    val isCurrencyLocked: Boolean,
    val nameLength: Int,
    val maxNameLength: Int,
    val isLoaded: Boolean,
    val isSaving: Boolean,
    val showsSaveError: Boolean,
    val showsNameError: Boolean,
    val canSave: Boolean,
    val showsOfflineNotice: Boolean,
)

class EditGroupPresenter(
    route: JsRoute,
) {
    private val editRoute = route.route as Route.EditGroup
    private val holder = ViewModelHolder(EditGroupViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(editRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsEditGroupState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsEditGroupState(
                name = state.name,
                initials = state.initials,
                color = state.color.code,
                type = state.type.code,
                types = state.types.toCodes(),
                currency = state.currency.code,
                currencies = state.currencies.toCurrencyCodes(),
                isCurrencyLocked = state.isCurrencyLocked,
                nameLength = state.nameLength,
                maxNameLength = state.maxNameLength,
                isLoaded = state.isLoaded,
                isSaving = state.isSaving,
                showsSaveError = state.showsSaveError,
                showsNameError = state.showsNameError,
                canSave = state.canSave,
                showsOfflineNotice = state.showsOfflineNotice,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: the ViewModel caps the name as it handles the event, so the field reads it back at once.
    fun getName(): String = viewModel.uiState.value.name

    fun close() {
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)
    }

    fun changeName(name: String) {
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged(name))
    }

    fun changeColor() {
        viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
    }

    fun selectType(code: String) {
        val type = GroupType.fromCodeOrNull(code) ?: return
        viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(type))
    }

    fun selectCurrency(code: String) {
        val currency = GroupCurrency.fromCodeOrNull(code) ?: return
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(currency))
    }

    fun save() {
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
