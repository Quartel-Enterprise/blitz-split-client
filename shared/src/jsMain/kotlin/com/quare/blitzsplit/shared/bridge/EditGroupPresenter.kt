@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyAmount
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsConfirmation
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsStatus
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyPersonChange
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: "R$ 203,80 a Bruno" in "O que muda" (13.3).
class JsSimplifyAmount(
    val to: JsSimplifyName,
    val amountMinor: Double,
)

// Why: a row of "O que muda", which the page words from the four lists in this order.
class JsSimplifyPersonChange(
    val memberId: String,
    val person: JsSimplifyName,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    // Why: "Diego · convidado … Não tem o app: avise por Cobrar.", a name nobody joined as.
    val isGuest: Boolean,
    val stopsPaying: Array<JsSimplifyAmount>,
    val pays: Array<JsSimplifyAmount>,
    val stopsReceivingFrom: Array<JsSimplifyName>,
    val receivesFrom: Array<JsSimplifyName>,
)

// Why: "Você tem 1 pagamento na fila: Elisa te pagou R$ 50,00." (13.3d)
class JsSimplifyQueuedPayment(
    val from: JsSimplifyName,
    val to: JsSimplifyName,
    val amountMinor: Double,
)

// Why: "Simplificar dívidas?" (13.3, 13.3c) or "Desligar Simplificar dívidas?" (13.3b), open while not null.
class JsSimplifyConfirmation(
    val isTurningOn: Boolean,
    val groupName: String,
    val paymentsBefore: Int,
    val paymentsAfter: Int,
    val people: Array<JsSimplifyPersonChange>,
    // Why: "Mais 5 pessoas mudam.", turning off.
    val othersChangingCount: Int,
    val queuedPayments: Array<JsSimplifyQueuedPayment>,
    val isAlreadyMinimal: Boolean,
    val hasGuestChanging: Boolean,
)

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
    // Why: the amounts of "Simplificar dívidas?" are in the group's currency.
    val fractionDigits: Int,
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
    // Why: "Simplificar dívidas" (13.1), as the group has it now; the switch shows it and never flips by itself.
    val simplifiesDebts: Boolean,
    // Why: "off", "on", "pending" or "needs-third-person", the line under the switch.
    val simplifyStatus: String,
    // Why: "Ligado · 5 pagamentos em vez de 7"; null while it is off.
    val simplification: JsDebtSimplification?,
    val canChangeSimplifyDebts: Boolean,
    // Why: "Sem internet: os pagamentos sugeridos já mudaram…" in place of the note (13.1d).
    val showsSimplifyOfflineNotice: Boolean,
    val simplifyConfirmation: JsSimplifyConfirmation?,
    val isChangingSimplifyDebts: Boolean,
    // Why: 13.1e's toast, until dismissed.
    val simplifyConflict: JsSimplifyConflict?,
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
                fractionDigits = state.currency.fractionDigits,
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
                simplifiesDebts = state.simplifiesDebts,
                simplifyStatus = when (state.simplifyStatus) {
                    SimplifyDebtsStatus.OFF -> "off"
                    SimplifyDebtsStatus.ON -> "on"
                    SimplifyDebtsStatus.PENDING -> "pending"
                    SimplifyDebtsStatus.NEEDS_THIRD_PERSON -> "needs-third-person"
                },
                simplification = state.simplification?.let(::toJsDebtSimplification),
                canChangeSimplifyDebts = state.canChangeSimplifyDebts,
                showsSimplifyOfflineNotice = state.showsSimplifyOfflineNotice,
                simplifyConfirmation = state.simplifyConfirmation?.let(::toJsSimplifyConfirmation),
                isChangingSimplifyDebts = state.isChangingSimplifyDebts,
                simplifyConflict = state.simplifyConflict?.let(::toJsSimplifyConflict),
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
        // Why: a picker never creates a currency this version doesn't know, unlike the chips of the group screens.
        val currency = GroupCurrency.fromCodeOrNull(code) ?: return
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(currency))
    }

    fun openDefaultSplit() {
        viewModel.onEvent(EditGroupUiEvent.OnDefaultSplitClicked)
    }

    fun save() {
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
    }

    // Why: the switch opens the confirmation; only "Simplificar" or "Desligar" there changes it.
    fun clickSimplifyDebts() {
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)
    }

    fun confirmSimplifyDebts() {
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)
    }

    fun dismissSimplifyDebts() {
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsDismissed)
    }

    // Why: "Como funciona" (13.2).
    fun openSimplifyDebtsInfo() {
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsInfoClicked)
    }

    fun dismissSimplifyConflict() {
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictDismissed)
    }

    // Why: the toast's "Ver na Atividade".
    fun openSimplifyConflictActivity() {
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictActivityClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsSimplifyName(name: SimplifyName): JsSimplifyName = JsSimplifyName(
    name = name.name,
    isMe = name.isMe,
)

private fun toJsSimplifyAmount(amount: SimplifyAmount): JsSimplifyAmount = JsSimplifyAmount(
    to = toJsSimplifyName(amount.to),
    amountMinor = amount.amountMinor.toDouble(),
)

private fun toJsSimplifyPersonChange(change: SimplifyPersonChange): JsSimplifyPersonChange = JsSimplifyPersonChange(
    memberId = change.memberId,
    person = toJsSimplifyName(change.person),
    initials = change.initials,
    color = change.avatarColor.code,
    photoUrl = change.photoUrl,
    isGuest = change.isGuest,
    stopsPaying = change.stopsPaying.map(::toJsSimplifyAmount).toTypedArray(),
    pays = change.pays.map(::toJsSimplifyAmount).toTypedArray(),
    stopsReceivingFrom = change.stopsReceivingFrom.map(::toJsSimplifyName).toTypedArray(),
    receivesFrom = change.receivesFrom.map(::toJsSimplifyName).toTypedArray(),
)

private fun toJsSimplifyConfirmation(confirmation: SimplifyDebtsConfirmation): JsSimplifyConfirmation =
    JsSimplifyConfirmation(
        isTurningOn = confirmation.isTurningOn,
        groupName = confirmation.groupName,
        paymentsBefore = confirmation.paymentsBefore,
        paymentsAfter = confirmation.paymentsAfter,
        people = confirmation.people.map(::toJsSimplifyPersonChange).toTypedArray(),
        othersChangingCount = confirmation.othersChangingCount,
        queuedPayments = confirmation.queuedPayments
            .map {
                JsSimplifyQueuedPayment(
                    from = toJsSimplifyName(it.from),
                    to = toJsSimplifyName(it.to),
                    amountMinor = it.amountMinor.toDouble(),
                )
            }.toTypedArray(),
        isAlreadyMinimal = confirmation.isAlreadyMinimal,
        hasGuestChanging = confirmation.hasGuestChanging,
    )
