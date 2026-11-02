@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillError
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.ShareBillViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsShareBillState(
    // Why: "Jantar no Ostradamus · R$ 642,00" under the title, the amount in the currency's minor unit.
    val description: String,
    val totalMinor: Double,
    val currency: String,
    val fractionDigits: Int,
    // Why: "Ana, Bruno e Carla entram com a própria conta…"; empty says "Quem está no grupo…".
    val memberNames: Array<String>,
    val link: String?,
    val displayLink: String?,
    // Why: "members" or "anyone".
    // Why: null, nothing picked, for a way a newer app added.
    val visibility: String?,
    val isLoading: Boolean,
    val isUpdating: Boolean,
    val hasFailed: Boolean,
    val isLinkCopied: Boolean,
    val showsOfflineNotice: Boolean,
    val canSendLink: Boolean,
    val canChangeLink: Boolean,
)

/*
 * Why: type is "copy" (link), "send" (link and description, for the message) or "error" (error: "update-failed" or
 * "disable-failed"); what a type doesn't use is null.
 */
class JsShareBillAction(
    val type: String,
    val link: String?,
    val description: String?,
    val error: String?,
)

class ShareBillPresenter(
    route: JsRoute,
) {
    private val shareRoute = route.route as Route.ShareBill
    private val holder = ViewModelHolder(ShareBillViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(shareRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsShareBillState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsShareBillState(
                description = state.description,
                totalMinor = state.totalMinor.toDouble(),
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                memberNames = state.memberNames.toTypedArray(),
                link = state.link,
                displayLink = state.displayLink,
                visibility = state.visibility?.code,
                isLoading = state.isLoading,
                isUpdating = state.isUpdating,
                hasFailed = state.hasFailed,
                isLinkCopied = state.isLinkCopied,
                showsOfflineNotice = state.showsOfflineNotice,
                canSendLink = state.canSendLink,
                canChangeLink = state.canChangeLink,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsShareBillAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                is ShareBillUiAction.CopyLink -> JsShareBillAction("copy", action.link, null, null)

                is ShareBillUiAction.SendLink -> JsShareBillAction("send", action.link, action.description, null)

                is ShareBillUiAction.ShowError -> JsShareBillAction(
                    type = "error",
                    link = null,
                    description = null,
                    error = when (action.error) {
                        ShareBillError.UPDATE_FAILED -> "update-failed"
                        ShareBillError.DISABLE_FAILED -> "disable-failed"
                    },
                )
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: "members" or "anyone".
    fun pickVisibility(code: String) {
        val visibility = BillLinkVisibility.fromCodeOrNull(code) ?: return
        viewModel.onEvent(ShareBillUiEvent.OnVisibilityPicked(visibility))
    }

    fun copy() {
        viewModel.onEvent(ShareBillUiEvent.OnCopyClicked)
    }

    fun send() {
        viewModel.onEvent(ShareBillUiEvent.OnSendClicked)
    }

    fun disable() {
        viewModel.onEvent(ShareBillUiEvent.OnDisableClicked)
    }

    fun retry() {
        viewModel.onEvent(ShareBillUiEvent.OnRetryClicked)
    }

    fun close() {
        viewModel.onEvent(ShareBillUiEvent.OnCloseClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
