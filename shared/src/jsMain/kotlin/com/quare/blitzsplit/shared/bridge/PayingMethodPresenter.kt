@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.CardTints
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiEvent
import com.quare.blitzsplit.feature.payingmethod.presentation.viewmodel.PayingMethodViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsPayingMethodState(
    val isEditing: Boolean,
    val isLoaded: Boolean,
    // Why: the PayingMethodType's code ("credit_card"); types lists the ones the form offers, in the order drawn.
    val type: String,
    val types: Array<String>,
    val name: String,
    // Why: the card's color code, a category's ("teal" or "#3A7BD5"), which the category color picker draws.
    val cardColor: String,
    val customColors: JsCustomColors,
    val showsCardColors: Boolean,
    val isNameRequired: Boolean,
    val nameMaxLength: Int,
    val isDefault: Boolean,
    val isSaving: Boolean,
    val isDeleteConfirmationShown: Boolean,
    val canSave: Boolean,
)

class PayingMethodPresenter(
    route: JsRoute,
) {
    private val payingMethodRoute = route.route as Route.PayingMethod
    private val holder = ViewModelHolder(PayingMethodViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(payingMethodRoute) }
    }
    private val viewModel get() = holder.viewModel

    val customColors: JsCustomColorActions = JsCustomColorActions(
        send = { viewModel.onEvent(PayingMethodUiEvent.OnCustomColor(it)) },
        current = { viewModel.uiState.value.customColors },
    )

    fun observeState(onChange: (JsPayingMethodState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsPayingMethodState(
                isEditing = state.isEditing,
                isLoaded = state.isLoaded,
                type = state.type.code,
                types = state.types.map(PayingMethodType::code).toTypedArray(),
                name = state.name,
                cardColor = state.cardColor.code,
                customColors = toJsCustomColors(state.customColors),
                showsCardColors = state.showsCardColors,
                isNameRequired = state.isNameRequired,
                nameMaxLength = state.nameMaxLength,
                isDefault = state.isDefault,
                isSaving = state.isSaving,
                isDeleteConfirmationShown = state.isDeleteConfirmationShown,
                canSave = state.canSave,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(PayingMethodUiEvent.OnBackClicked)
    }

    fun selectType(code: String) {
        val type = PayingMethodType.ofCode(code) ?: return
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(type))
    }

    fun changeName(name: String) {
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged(name))
    }

    fun selectCardColor(code: String) {
        val color = CategoryTint.fromCodeOrNull(code) ?: return
        viewModel.onEvent(PayingMethodUiEvent.OnCardColorSelected(color))
    }

    fun changeDefault(isDefault: Boolean) {
        viewModel.onEvent(PayingMethodUiEvent.OnDefaultChanged(isDefault))
    }

    fun save() {
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)
    }

    fun delete() {
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)
    }

    fun confirmDelete() {
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteConfirmed)
    }

    fun dismissDelete() {
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

// Why: one of the person's payment methods as every web screen draws it: its type's code, its name, a card's color.
class JsPayingMethod(
    val id: String,
    val type: String,
    // Why: empty for none, which the UI replaces with the type's name.
    val name: String,
    // Why: a card's color code, a category's ("teal" or "#3A7BD5"), null for anything but a card.
    val cardColor: String?,
    val isDefault: Boolean,
)

internal fun com.quare.blitzsplit.core.expenses.domain.model.PayingMethod.toJsPayingMethod(): JsPayingMethod =
    JsPayingMethod(
        id = id,
        type = type.code,
        name = name,
        cardColor = cardColor?.code,
        isDefault = isDefault,
    )

// Why: a card tile's fill as a CSS color ("#RRGGBB"), the tone Android and the Apple app draw (CardTints.fillRgbOf).
fun getCardFill(code: String?): String = CustomColor(CardTints.fillRgbOf(CardTints.fromCodeOrNull(code))).hex
