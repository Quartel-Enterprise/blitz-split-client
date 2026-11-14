@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.ReceiptPhotoSource
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsBillPerson(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val hasJoined: Boolean,
)

class JsSplitRow(
    val person: JsBillPerson,
    val isIncluded: Boolean,
    val valueText: String,
    val parts: Double,
    val shareMinor: Double?,
    // Why: By income's percentage the income gives, shown beside the share; null in any other mode.
    val incomePercent: Double?,
)

// Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly.
class JsSplitEditor(
    // Why: the SplitMode's code: "equal", "amount", "percent", "shares", "items" or "income".
    val mode: String,
    val rows: Array<JsSplitRow>,
    val totalMinor: Double,
    val addsUp: Boolean,
    val included: Array<JsBillPerson>,
    val excluded: Array<JsBillPerson>,
    val includedCount: Int,
    val isEveryoneIncluded: Boolean,
    val perPersonMinor: Double,
    val assignedMinor: Double,
    val remainingMinor: Double,
    val withoutValue: Array<JsBillPerson>,
    val percentSum: Double,
    val remainingPercent: Double,
    val totalParts: Double,
    val partValueMinor: Double,
    val statusTitle: JsSplitFact,
    val statusDetails: Array<JsSplitFact>,
)

/*
 * Why: the group's "Divisão padrão" a new bill opens with. mode is the DefaultSplitMode's code ("shares"), values the
 * parts or percentages of who is in it ("3 para 1"), excluded who an "Igual" default leaves out, editorMode the
 * SplitMode's code it gives the bill ("percent" for "Pela renda").
 */
class JsBillDefaultSplit(
    val mode: String,
    val editorMode: String,
    val values: Array<Double>,
    val excluded: Array<JsBillPerson>,
)

// Why: a group "Rachar em qual grupo?" lists.
class JsBillGroup(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val memberCount: Int,
)

// Why: changedAt in epoch millis, which the page writes as how long ago.
class JsBillConflict(
    val changerName: String?,
    val changerIsMe: Boolean,
    val changedAt: Double,
)

/*
 * Why: "Ler cupom fiscal" (7.1e to 7.1g). status is "reading", "read" or "failed"; itemCount is "14 itens";
 * hasPhoto is "Foto do cupom", false when the file couldn't be opened as an image.
 */
class JsBillReceipt(
    val status: String,
    val itemCount: Int,
    val hasPhoto: Boolean,
)

class JsSplitBillState(
    val isEditing: Boolean,
    val groupId: String,
    val groupName: String,
    val groupInitials: String,
    val groupColor: String,
    val groupPhotoUrl: String?,
    val groups: Array<JsBillGroup>,
    val canChangeGroup: Boolean,
    val canOpenGroup: Boolean,
    // Why: the group "Rachar em qual grupo?" marks while it is open, null while it is closed.
    val groupSelection: String?,
    val currency: String,
    val fractionDigits: Int,
    val currencies: Array<String>,
    val cryptoCurrencies: Array<String>,
    val canChangeCurrency: Boolean,
    val people: Array<JsBillPerson>,
    val amountText: String,
    val description: String,
    val showsAmountError: Boolean,
    val showsDescriptionError: Boolean,
    val amountMinor: Double?,
    val isAmountValid: Boolean,
    val payer: JsBillPerson?,
    val payerIsMe: Boolean,
    val paidWith: JsPaidWith?,
    val isPayerFormerMember: Boolean,
    // Why: who paid, whom Dividir marks as keeping the cents.
    val payerIds: Array<String>,
    val severalPayers: JsPayerAmounts?,
    // Why: "Quem pagou" is open, with payerSelection marked, or "Várias pessoas" while it is null.
    val isPayerOpen: Boolean,
    val payerSelection: String?,
    val payerAmounts: JsPayerAmounts?,
    val spentAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val spentDay: String,
    val latestSpentAt: Double,
    val split: JsSplitEditor,
    val splitEditor: JsSplitEditor?,
    // Why: "Itens da conta" in Dividir's place, "Item" over it, and their questions; null while closed.
    val itemsEditor: JsItemsEditor?,
    val itemEditor: JsItemEditor?,
    val itemDeletion: JsItemDeletion?,
    val feeEditor: JsFeeEditor?,
    // Why: what "Divisão" says under its title, "Padrão do grupo" first while it follows the group's default.
    val splitSummary: Array<JsSplitFact>,
    val defaultSplit: JsBillDefaultSplit?,
    val showsDefaultSplitNotice: Boolean,
    val showsDefaultSplitHint: Boolean,
    val isSplitEditorOffDefault: Boolean,
    // Why: Dividir's "Usar como padrão do grupo" (beta feedback 11), shown while offered.
    val offersSplitAsDefault: Boolean,
    val isSplitEditorAsDefaultChecked: Boolean,
    val myShareMinor: Double?,
    val refundMinor: Double?,
    val canSave: Boolean,
    val isSaving: Boolean,
    val showsOfflineNotice: Boolean,
    val conflict: JsBillConflict?,
    val settledPair: JsSettledPair?,
    val showsAnyoneCanEditHint: Boolean,
    val showsProportionalAmountHint: Boolean,
    val deletion: JsBillDeletion?,
    val canDelete: Boolean,
    val isDeleting: Boolean,
    val isLoaded: Boolean,
    // Why: the bill's category and "Categoria" (7.7, 7.8).
    val categories: JsBillCategories,
    val receipt: JsBillReceipt?,
    val showsScanReceipt: Boolean,
    val canScanReceipt: Boolean,
    // Why: "Ler cupom fiscal" needs the internet; its help says so offline.
    val isOnline: Boolean,
    // Why: "Tirar foto" or "Escolher arquivo", asked once "Rachar em qual grupo?" was answered (beta feedback, part 2, 7).
    val showsReceiptSourceChoice: Boolean,
    // Why: the picker picked on that choice, to open: "camera" or "files"; null for none.
    val receiptSource: String?,
    // Why: why the receipt read nothing (7.1g): "unreadable", "reader_unavailable", "limit_reached" or "offline".
    val receiptError: String?,
    val offersReceiptRetake: Boolean,
    val offersReceiptRetry: Boolean,
    // Why: every field but "Foto do cupom" shows disabled while the receipt is read (7.1e).
    val isFormLocked: Boolean,
)

class SplitBillPresenter(
    route: JsRoute,
) {
    private val billRoute = route.route as Route.SplitBill
    private val holder = ViewModelHolder(SplitBillViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(billRoute) }
    }
    private val viewModel get() = holder.viewModel

    // Why: "Itens da conta" and "Item", on this same ViewModel.
    val items: SplitItemsPresenter = SplitItemsPresenter { holder.viewModel }

    // Why: "Categoria" and "Nova categoria", on this same ViewModel.
    val categories: SplitCategoriesPresenter = SplitCategoriesPresenter { holder.viewModel }

    fun observeState(onChange: (JsSplitBillState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsSplitBillState(
                isEditing = state.isEditing,
                groupId = state.groupId,
                groupName = state.groupName,
                groupInitials = state.groupInitials,
                groupColor = state.groupColor.code,
                groupPhotoUrl = state.groupPhotoUrl,
                groups = state.groups
                    .map {
                        JsBillGroup(
                            id = it.id,
                            name = it.name,
                            initials = it.initials,
                            color = it.color.code,
                            photoUrl = it.photoUrl,
                            memberCount = it.memberCount,
                        )
                    }.toTypedArray(),
                canChangeGroup = state.canChangeGroup,
                canOpenGroup = state.canOpenGroup,
                groupSelection = state.groupSelection?.groupId,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                currencies = state.currencies.toCurrencyCodes(),
                cryptoCurrencies = state.currencies.toCryptoCurrencyCodes(),
                canChangeCurrency = state.canChangeCurrency,
                people = state.people.map(::toJsBillPerson).toTypedArray(),
                amountText = state.amountText,
                description = state.description,
                showsAmountError = state.showsAmountError,
                showsDescriptionError = state.showsDescriptionError,
                amountMinor = state.amountMinor?.toDouble(),
                isAmountValid = state.isAmountValid,
                payer = state.payer?.let(::toJsBillPerson),
                payerIsMe = state.payerIsMe,
                paidWith = toJsPaidWithOrNull(state),
                isPayerFormerMember = state.isPayerFormerMember,
                payerIds = state.payerIds.toTypedArray(),
                severalPayers = state.severalPayers?.let(::toJsPayerAmounts),
                isPayerOpen = state.payerSelection != null,
                payerSelection = state.payerSelection?.memberId,
                payerAmounts = state.payerAmounts?.let(::toJsPayerAmounts),
                spentAt = state.spentAt.toDouble(),
                spentDay = toJsRelativeDay(state.spentDay),
                latestSpentAt = state.latestSpentAt.toDouble(),
                split = toJsSplitEditor(state.split),
                splitEditor = state.splitEditor?.let(::toJsSplitEditor),
                itemsEditor = state.itemsEditor?.let(::toJsItemsEditor),
                itemEditor = state.itemEditor?.let(::toJsItemEditor),
                itemDeletion = state.itemDeletion?.let(::toJsItemDeletion),
                feeEditor = state.feeEditor?.let(::toJsFeeEditor),
                splitSummary = state.splitSummary.map(::toJsSplitFact).toTypedArray(),
                defaultSplit = state.defaultSplit?.let { default ->
                    JsBillDefaultSplit(
                        mode = default.mode.code,
                        editorMode = default.editor.mode.code,
                        values = default.values.map { it.toDouble() }.toTypedArray(),
                        excluded = default.excluded.map(::toJsBillPerson).toTypedArray(),
                    )
                },
                showsDefaultSplitNotice = state.showsDefaultSplitNotice,
                showsDefaultSplitHint = state.showsDefaultSplitHint,
                isSplitEditorOffDefault = state.isSplitEditorOffDefault,
                offersSplitAsDefault = state.offersSplitAsDefault,
                isSplitEditorAsDefaultChecked = state.isSplitEditorAsDefaultChecked,
                myShareMinor = state.myShareMinor?.toDouble(),
                refundMinor = state.refundMinor?.toDouble(),
                canSave = state.canSave,
                isSaving = state.isSaving,
                showsOfflineNotice = state.showsOfflineNotice,
                conflict = state.conflict?.let {
                    JsBillConflict(
                        changerName = it.changerName,
                        changerIsMe = it.changerIsMe,
                        changedAt = it.changedAt.toDouble(),
                    )
                },
                settledPair = state.settledPair?.let(::toJsSettledPair),
                showsAnyoneCanEditHint = state.showsAnyoneCanEditHint,
                showsProportionalAmountHint = state.showsProportionalAmountHint,
                deletion = state.deletion?.let(::toJsBillDeletion),
                canDelete = state.canDelete,
                isDeleting = state.isDeleting,
                isLoaded = state.isLoaded,
                categories = toJsBillCategories(state),
                receipt = state.receipt?.let {
                    JsBillReceipt(
                        status = it.status.name.lowercase(),
                        itemCount = it.itemCount,
                        hasPhoto = it.hasPhoto,
                    )
                },
                showsScanReceipt = state.showsScanReceipt,
                canScanReceipt = state.canScanReceipt,
                isOnline = state.isOnline,
                showsReceiptSourceChoice = state.showsReceiptSourceChoice,
                receiptSource = state.shownReceiptSource?.name?.lowercase(),
                receiptError = state.receiptError?.name?.lowercase(),
                offersReceiptRetake = state.offersReceiptRetake,
                offersReceiptRetry = state.offersReceiptRetry,
                isFormLocked = state.isFormLocked,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "save-error" or "delete-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                SplitBillUiAction.ShowSaveError -> "save-error"
                SplitBillUiAction.ShowDeleteError -> "delete-error"
                SplitBillUiAction.ShowCategorySaveError -> "category-save-error"
                SplitBillUiAction.ShowCategoryDeleteError -> "category-delete-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: the file picked, as an Int8Array of its bytes.
    fun receiptPicked(photo: ByteArray) {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(photo))
    }

    // Why: "Ler cupom fiscal" and "Tirar outra foto", which ask where the photo comes from.
    fun scanReceipt() {
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
    }

    // Why: "Tirar foto" on that choice, the browser's camera.
    fun takeReceiptPhoto() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
    }

    // Why: "Escolher foto" on that choice.
    fun chooseReceiptFile() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.FILES))
    }

    // Why: "Ler cupom fiscal" where the browser lists no camera, which opens the file chooser without asking.
    fun skipReceiptChoice() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.FILES))
    }

    // Why: the choice or the camera closed without a photo, or the file chooser opened, which reports no close.
    fun dismissReceiptPicker() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPickerDismissed)
    }

    // Why: "Tentar de novo", the same photo sent again.
    fun retryReceipt() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)
    }

    fun removeReceipt() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)
    }

    // Why: "14 itens", Dividir straight on "Itens".
    fun openReceiptItems() {
        viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)
    }

    fun deleteBill() {
        viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)
    }

    fun confirmDelete() {
        viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)
    }

    fun dismissDelete() {
        viewModel.onEvent(SplitBillUiEvent.OnDeleteDismissed)
    }

    fun changeCurrency(code: String) {
        GroupCurrency.fromCodeOrNull(code)?.let { viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(it)) }
    }

    fun changeAmount(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(text))
    }

    // Why: the ViewModel filters the amount as it handles it, so the field reads it back in the same keystroke.
    fun getAmountText(): String = viewModel.uiState.value.amountText

    fun changeDescription(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(text))
    }

    fun getDescription(): String = viewModel.uiState.value.description

    // Why: "Change" beside the group, which asks "Rachar em qual grupo?".
    fun openGroup() {
        viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
    }

    // Why: the group itself, whose page opens over the form, which stays mounted with what was typed.
    fun openGroupScreen() {
        viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)
    }

    fun pickGroup(groupId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(groupId))
    }

    fun confirmGroup() {
        viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
    }

    fun dismissGroup() {
        viewModel.onEvent(SplitBillUiEvent.OnGroupDismissed)
    }

    fun openPayer() {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
    }

    fun pickPayer(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(memberId))
    }

    fun pickPaidWith(methodId: String?) = viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(methodId))

    fun confirmPayer() {
        viewModel.onEvent(SplitBillUiEvent.OnPayerDone)
    }

    fun dismissPayer() {
        viewModel.onEvent(SplitBillUiEvent.OnPayerDismissed)
    }

    fun openSeveralPayers() {
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
    }

    fun changePayerAmount(
        memberId: String,
        text: String,
    ) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(memberId, text))
    }

    // Why: the field reads the filtered text back in the same keystroke.
    fun getPayerAmount(memberId: String): String = viewModel.uiState.value.payerAmounts
        ?.rows
        ?.firstOrNull { it.memberId == memberId }
        ?.amountText
        .orEmpty()

    fun confirmSeveralPayers() {
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)
    }

    fun dismissSeveralPayers() {
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDismissed)
    }

    fun openSpentAt() {
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)
    }

    // Why: the date input's day, its month from 1 for January.
    fun pickSpentDay(
        year: Int,
        month: Int,
        day: Int,
    ) {
        viewModel.onEvent(SplitBillUiEvent.OnSpentDayPicked(year, month, day))
    }

    fun pickSpentTime(
        hour: Int,
        minute: Int,
    ) {
        viewModel.onEvent(SplitBillUiEvent.OnSpentTimePicked(hour, minute))
    }

    fun openSplit() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
    }

    fun changeSplitMode(code: String) {
        val mode = SplitMode.fromCodeOrNull(code) ?: return
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(mode))
    }

    fun togglePerson(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(memberId))
    }

    fun toggleEveryone() {
        viewModel.onEvent(SplitBillUiEvent.OnEveryoneToggled)
    }

    fun changePersonValue(
        memberId: String,
        text: String,
    ) {
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(memberId, text))
    }

    // Why: the field reads the filtered text back in the same keystroke.
    fun getPersonValue(memberId: String): String = viewModel.uiState.value.splitEditor
        ?.rows
        ?.firstOrNull { it.person.id == memberId }
        ?.valueText
        .orEmpty()

    fun increaseParts(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPartsIncreased(memberId))
    }

    fun decreaseParts(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPartsDecreased(memberId))
    }

    fun resetSplitToDefault() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitResetToDefault)
    }

    fun toggleSplitAsDefault() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitAsDefaultToggled)
    }

    fun confirmSplit() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
    }

    fun dismissSplit() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitDismissed)
    }

    fun save() {
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
    }

    fun close() {
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

internal fun toJsBillPerson(person: BillPerson): JsBillPerson = JsBillPerson(
    id = person.id,
    name = person.name,
    initials = person.initials,
    color = person.avatarColor.code,
    photoUrl = person.photoUrl,
    isMe = person.isMe,
    hasJoined = person.hasJoined,
)

private fun toJsSplitRow(
    row: SplitRow,
    incomePercent: Long?,
): JsSplitRow = JsSplitRow(
    person = toJsBillPerson(row.person),
    isIncluded = row.isIncluded,
    valueText = row.valueText,
    parts = row.parts.toDouble(),
    shareMinor = row.shareMinor?.toDouble(),
    incomePercent = incomePercent?.toDouble(),
)

private fun toJsSplitEditor(editor: SplitEditor): JsSplitEditor = JsSplitEditor(
    mode = editor.mode.code,
    rows = editor.rows
        .map { row -> toJsSplitRow(row, editor.incomePercents[row.person.id]) }
        .toTypedArray(),
    totalMinor = editor.totalMinor.toDouble(),
    addsUp = editor.addsUp,
    included = editor.included.map(::toJsBillPerson).toTypedArray(),
    excluded = editor.excluded.map(::toJsBillPerson).toTypedArray(),
    includedCount = editor.includedCount,
    isEveryoneIncluded = editor.isEveryoneIncluded,
    perPersonMinor = editor.perPersonMinor.toDouble(),
    assignedMinor = editor.assignedMinor.toDouble(),
    remainingMinor = editor.remainingMinor.toDouble(),
    withoutValue = editor.withoutValue.map(::toJsBillPerson).toTypedArray(),
    percentSum = editor.percentSum.toDouble(),
    remainingPercent = editor.remainingPercent.toDouble(),
    totalParts = editor.totalParts.toDouble(),
    partValueMinor = editor.partValueMinor.toDouble(),
    statusTitle = toJsSplitFact(editor.statusTitle),
    statusDetails = editor.statusDetails.map(::toJsSplitFact).toTypedArray(),
)
