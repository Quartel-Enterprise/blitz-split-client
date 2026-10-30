@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsBillPayer(
    val name: String,
    val initials: String,
    val color: String,
    val isMe: Boolean,
)

// Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly; name null for "Ex-membro".
class JsBillShareRow(
    val memberId: String,
    val name: String?,
    val initials: String,
    val color: String,
    val isMe: Boolean,
    val hasJoined: Boolean,
    val paidMinor: Double?,
    val shareMinor: Double?,
)

/*
 * Why: the line under "Sua parte", flat. kind is "owes-payer" (payerName set) or "owed-by" (names, eachMinor when
 * they all owe the same, and totalMinor set); a null name in names is "Ex-membro".
 */
class JsMyPartNote(
    val kind: String,
    val payerName: String?,
    val names: Array<String?>,
    val eachMinor: Double?,
    val totalMinor: Double?,
)

class JsBillDetailState(
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val description: String,
    val totalMinor: Double,
    val spentAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val spentDay: String,
    val payer: JsBillPayer?,
    val isPending: Boolean,
    val myBalanceMinor: Double?,
    val myPartNote: JsMyPartNote?,
    val showsMyPart: Boolean,
    // Why: the SplitMode's code: "equal", "amount", "percent" or "shares".
    val splitMode: String,
    val participantCount: Int,
    val people: Array<JsBillShareRow>,
    val deletion: JsBillDeletion?,
    val isDeleting: Boolean,
    val areActionsEnabled: Boolean,
    val isLoaded: Boolean,
)

class BillDetailPresenter(
    route: JsRoute,
) {
    private val billRoute = route.route as Route.BillDetail
    private val holder = ViewModelHolder(BillDetailViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(billRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsBillDetailState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsBillDetailState(
                groupName = state.groupName,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                description = state.description,
                totalMinor = state.totalMinor.toDouble(),
                spentAt = state.spentAt.toDouble(),
                spentDay = toJsRelativeDay(state.spentDay),
                payer = state.payer?.let {
                    JsBillPayer(
                        name = it.name,
                        initials = it.initials,
                        color = it.avatarColor.code,
                        isMe = it.isMe,
                    )
                },
                isPending = state.isPending,
                myBalanceMinor = state.myBalanceMinor?.toDouble(),
                myPartNote = state.myPartNote?.let(::toJsMyPartNote),
                showsMyPart = state.showsMyPart,
                splitMode = state.splitMode.code,
                participantCount = state.participantCount,
                people = state.people.map(::toJsBillShareRow).toTypedArray(),
                deletion = state.deletion?.let(::toJsBillDeletion),
                isDeleting = state.isDeleting,
                areActionsEnabled = state.areActionsEnabled,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "delete-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                BillDetailUiAction.ShowDeleteError -> "delete-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(BillDetailUiEvent.OnBackClicked)
    }

    fun edit() {
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
    }

    fun deleteBill() {
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
    }

    fun confirmDelete() {
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)
    }

    fun dismissDelete() {
        viewModel.onEvent(BillDetailUiEvent.OnDeleteDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsBillShareRow(row: BillShareRow): JsBillShareRow = JsBillShareRow(
    memberId = row.memberId,
    name = row.name,
    initials = row.initials,
    color = row.avatarColor.code,
    isMe = row.isMe,
    hasJoined = row.hasJoined,
    paidMinor = row.paidMinor?.toDouble(),
    shareMinor = row.shareMinor?.toDouble(),
)

private fun toJsMyPartNote(note: MyPartNote): JsMyPartNote = when (note) {
    is MyPartNote.OwesPayer -> JsMyPartNote(
        kind = "owes-payer",
        payerName = note.payerName,
        names = emptyArray(),
        eachMinor = null,
        totalMinor = null,
    )

    is MyPartNote.OwedBy -> JsMyPartNote(
        kind = "owed-by",
        payerName = null,
        names = note.names.toTypedArray(),
        eachMinor = note.eachMinor?.toDouble(),
        totalMinor = note.totalMinor.toDouble(),
    )
}
