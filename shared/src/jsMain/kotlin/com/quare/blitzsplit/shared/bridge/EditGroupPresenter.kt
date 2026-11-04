@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AvatarColor
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
    // Why: "Cor do ícone": the colors in the order drawn, and the one marked, null while the photo stands for the icon.
    val colors: Array<String>,
    val selectedColor: String?,
    // Why: drawn in place of the color and the initials, null for none.
    val photoUrl: String?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, null while closed.
    val photoCrop: JsPhotoCropState?,
    // Why: the GroupType's code ("trip"); types lists them in the order the UIs draw them.
    val type: String,
    val types: Array<String>,
    // Why: the ISO 4217 code ("BRL"); each UI names it in its own language.
    val currency: String,
    val currencies: Array<String>,
    // Why: the ones among currencies "Moeda" lists under "Criptomoedas".
    val cryptoCurrencies: Array<String>,
    val isCurrencyLocked: Boolean,
    // Why: the DefaultSplitMode's code ("shares"), the value of the "Divisão padrão" row.
    val defaultSplitMode: String,
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

    val crop: PhotoCropPresenter =
        PhotoCropPresenter { event -> viewModel.onEvent(EditGroupUiEvent.OnPhotoCrop(event)) }

    fun observeState(onChange: (JsEditGroupState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsEditGroupState(
                name = state.name,
                initials = state.initials,
                color = state.color.code,
                colors = state.colors.map(AvatarColor::code).toTypedArray(),
                selectedColor = state.selectedColor?.code,
                photoUrl = state.photoUrl,
                isPreparingPhoto = state.isPreparingPhoto,
                showsPhotoError = state.showsPhotoError,
                photoCrop = state.photoCrop?.toJs(),
                type = state.type.code,
                types = state.types.toCodes(),
                currency = state.currency.code,
                currencies = state.currencies.toCurrencyCodes(),
                cryptoCurrencies = state.currencies.toCryptoCurrencyCodes(),
                isCurrencyLocked = state.isCurrencyLocked,
                defaultSplitMode = state.defaultSplitMode.code,
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

    // Why: the color takes the photo's place, as "Usar este ícone" does on the phones.
    fun selectColor(code: String) {
        val color = AvatarColor.fromCodeOrNull(code) ?: return
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(color))
    }

    // Why: a Kotlin ByteArray is an Int8Array in JS, so "Usar uma foto"'s bytes cross without a copy.
    fun photoPicked(source: ByteArray) {
        viewModel.onEvent(EditGroupUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(EditGroupUiEvent.OnPhotoUnreadable)
    }

    fun selectType(code: String) {
        val type = GroupType.fromCodeOrNull(code) ?: return
        viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(type))
    }

    fun selectCurrency(code: String) {
        val currency = GroupCurrency.fromCodeOrNull(code) ?: return
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(currency))
    }

    fun openDefaultSplit() {
        viewModel.onEvent(EditGroupUiEvent.OnDefaultSplitClicked)
    }

    fun save() {
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
