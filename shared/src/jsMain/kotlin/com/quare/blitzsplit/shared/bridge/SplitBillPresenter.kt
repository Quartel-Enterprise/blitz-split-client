@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact
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
    val isMe: Boolean,
    val hasJoined: Boolean,
)

class JsSplitRow(
    val person: JsBillPerson,
    val isIncluded: Boolean,
    val valueText: String,
    val parts: Double,
    val shareMinor: Double?,
)

/*
 * Why: a SplitFact, flat. kind is "each", "nobody", "nobody-checked", "nobody-has-parts", "among", "out",
 * "amount-left", "amount-over", "amount-adds-up", "parts-match-total", "amount-so-far", "without-value",
 * "percent-left", "percent-over", "percent-adds-up", "parts" or "person-parts", and only that fact's fields are set:
 * amountMinor is also the assigned amount of "amount-so-far" and the part's value of "parts", count the people of
 * "among" and the parts of "parts" and "person-parts".
 */
class JsSplitFact(
    val kind: String,
    val amountMinor: Double?,
    val totalMinor: Double?,
    val count: Double?,
    val percent: Double?,
    val people: Array<JsBillPerson>,
    val person: JsBillPerson?,
)

// Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly.
class JsSplitEditor(
    // Why: the SplitMode's code: "equal", "amount", "percent" or "shares".
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
    val summary: Array<JsSplitFact>,
)

// Why: changedAt in epoch millis, which the page writes as how long ago.
class JsBillConflict(
    val changerName: String?,
    val changerIsMe: Boolean,
    val changedAt: Double,
)

class JsSplitBillState(
    val isEditing: Boolean,
    val groupName: String,
    val groupInitials: String,
    val groupColor: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val people: Array<JsBillPerson>,
    val amountText: String,
    val description: String,
    val showsAmountError: Boolean,
    val showsDescriptionError: Boolean,
    val amountMinor: Double?,
    val isAmountValid: Boolean,
    val payer: JsBillPerson?,
    val payerIsMe: Boolean,
    val payerSelection: String?,
    val spentAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val spentDay: String,
    val latestSpentAt: Double,
    val split: JsSplitEditor,
    val splitEditor: JsSplitEditor?,
    val myShareMinor: Double?,
    val refundMinor: Double?,
    val canSave: Boolean,
    val isSaving: Boolean,
    val showsOfflineNotice: Boolean,
    val conflict: JsBillConflict?,
    val showsAnyoneCanEditHint: Boolean,
    val deletion: JsBillDeletion?,
    val canDelete: Boolean,
    val isDeleting: Boolean,
    val isLoaded: Boolean,
)

class SplitBillPresenter(
    route: JsRoute,
) {
    private val billRoute = route.route as Route.SplitBill
    private val holder = ViewModelHolder(SplitBillViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(billRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsSplitBillState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsSplitBillState(
                isEditing = state.isEditing,
                groupName = state.groupName,
                groupInitials = state.groupInitials,
                groupColor = state.groupColor.code,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                people = state.people.map(::toJsBillPerson).toTypedArray(),
                amountText = state.amountText,
                description = state.description,
                showsAmountError = state.showsAmountError,
                showsDescriptionError = state.showsDescriptionError,
                amountMinor = state.amountMinor?.toDouble(),
                isAmountValid = state.isAmountValid,
                payer = state.payer?.let(::toJsBillPerson),
                payerIsMe = state.payerIsMe,
                payerSelection = state.payerSelection,
                spentAt = state.spentAt.toDouble(),
                spentDay = toJsRelativeDay(state.spentDay),
                latestSpentAt = state.latestSpentAt.toDouble(),
                split = toJsSplitEditor(state.split),
                splitEditor = state.splitEditor?.let(::toJsSplitEditor),
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
                showsAnyoneCanEditHint = state.showsAnyoneCanEditHint,
                deletion = state.deletion?.let(::toJsBillDeletion),
                canDelete = state.canDelete,
                isDeleting = state.isDeleting,
                isLoaded = state.isLoaded,
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
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
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

    fun changeAmount(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(text))
    }

    // Why: the ViewModel filters the amount as it handles it, so the field reads it back in the same keystroke.
    fun getAmountText(): String = viewModel.uiState.value.amountText

    fun changeDescription(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(text))
    }

    fun getDescription(): String = viewModel.uiState.value.description

    fun openPayer() {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
    }

    fun pickPayer(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(memberId))
    }

    fun confirmPayer() {
        viewModel.onEvent(SplitBillUiEvent.OnPayerDone)
    }

    fun dismissPayer() {
        viewModel.onEvent(SplitBillUiEvent.OnPayerDismissed)
    }

    fun openSpentAt() {
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)
    }

    fun changeSpentAt(epochMillis: Double) {
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(epochMillis.toLong()))
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

private fun toJsBillPerson(person: BillPerson): JsBillPerson = JsBillPerson(
    id = person.id,
    name = person.name,
    initials = person.initials,
    color = person.avatarColor.code,
    isMe = person.isMe,
    hasJoined = person.hasJoined,
)

private fun toJsSplitRow(row: SplitRow): JsSplitRow = JsSplitRow(
    person = toJsBillPerson(row.person),
    isIncluded = row.isIncluded,
    valueText = row.valueText,
    parts = row.parts.toDouble(),
    shareMinor = row.shareMinor?.toDouble(),
)

private fun toJsSplitEditor(editor: SplitEditor): JsSplitEditor = JsSplitEditor(
    mode = editor.mode.code,
    rows = editor.rows.map(::toJsSplitRow).toTypedArray(),
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
    summary = editor.summary.map(::toJsSplitFact).toTypedArray(),
)

private fun toJsSplitFact(fact: SplitFact): JsSplitFact {
    val kind = when (fact) {
        is SplitFact.Each -> "each"
        SplitFact.Nobody -> "nobody"
        is SplitFact.NobodyChecked -> "nobody-checked"
        is SplitFact.NobodyHasParts -> "nobody-has-parts"
        is SplitFact.Among -> "among"
        is SplitFact.Out -> "out"
        is SplitFact.AmountLeft -> "amount-left"
        is SplitFact.AmountOver -> "amount-over"
        is SplitFact.AmountAddsUp -> "amount-adds-up"
        SplitFact.PartsMatchTotal -> "parts-match-total"
        is SplitFact.AmountSoFar -> "amount-so-far"
        is SplitFact.WithoutValue -> "without-value"
        is SplitFact.PercentLeft -> "percent-left"
        is SplitFact.PercentOver -> "percent-over"
        SplitFact.PercentAddsUp -> "percent-adds-up"
        is SplitFact.Parts -> "parts"
        is SplitFact.PersonParts -> "person-parts"
    }
    val amountMinor = when (fact) {
        is SplitFact.Each -> fact.amountMinor
        is SplitFact.AmountLeft -> fact.amountMinor
        is SplitFact.AmountOver -> fact.amountMinor
        is SplitFact.AmountSoFar -> fact.assignedMinor
        is SplitFact.Parts -> fact.partMinor
        else -> null
    }
    val totalMinor = when (fact) {
        is SplitFact.NobodyChecked -> fact.totalMinor
        is SplitFact.NobodyHasParts -> fact.totalMinor
        is SplitFact.Among -> fact.totalMinor
        is SplitFact.AmountAddsUp -> fact.totalMinor
        is SplitFact.AmountSoFar -> fact.totalMinor
        else -> null
    }
    val count = when (fact) {
        is SplitFact.Among -> fact.count.toLong()
        is SplitFact.Parts -> fact.count
        is SplitFact.PersonParts -> fact.parts
        else -> null
    }
    val percent = when (fact) {
        is SplitFact.PercentLeft -> fact.percent
        is SplitFact.PercentOver -> fact.percent
        else -> null
    }
    val people = when (fact) {
        is SplitFact.Out -> fact.people
        is SplitFact.WithoutValue -> fact.people
        else -> emptyList()
    }
    return JsSplitFact(
        kind = kind,
        amountMinor = amountMinor?.toDouble(),
        totalMinor = totalMinor?.toDouble(),
        count = count?.toDouble(),
        percent = percent?.toDouble(),
        people = people.map(::toJsBillPerson).toTypedArray(),
        person = (fact as? SplitFact.PersonParts)?.person?.let(::toJsBillPerson),
    )
}
