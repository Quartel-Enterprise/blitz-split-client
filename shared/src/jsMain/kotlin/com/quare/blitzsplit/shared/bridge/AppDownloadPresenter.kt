@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiAction
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiEvent
import com.quare.blitzsplit.feature.appdownload.presentation.viewmodel.AppDownloadViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsAppDownloadState(
    val isLoaded: Boolean,
    val link: String,
    // Why: the QR code's modules row by row, qrSize by qrSize; empty until read.
    val qrSize: Int,
    val qrModules: Array<Boolean>,
    // Why: StoreListing's name, "BOTH", "GOOGLE_PLAY" or "APP_STORE", for the copy.
    val stores: String,
    // Why: "You've used the app on your phone since Oct 12" (14.2b); null while it never signed in on one.
    val phoneUseSinceMillis: Double?,
    // Why: AccountProvider's code, null without Google or Apple.
    val accountProvider: String?,
    val showsDownloadedButton: Boolean,
)

// Why: "copy" carries the link for the clipboard.
class JsAppDownloadAction(
    val type: String,
    val link: String,
)

class AppDownloadPresenter(
    route: JsRoute,
) {
    private val appDownloadRoute = route.route as Route.AppDownload
    private val holder = ViewModelHolder(AppDownloadViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(appDownloadRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsAppDownloadState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsAppDownloadState(
                isLoaded = state.isLoaded,
                link = state.link,
                qrSize = state.qrCode?.size ?: 0,
                qrModules = toJsModules(state.qrCode),
                stores = state.stores.name,
                phoneUseSinceMillis = state.phoneUseSinceMillis?.toDouble(),
                accountProvider = state.accountProvider?.code,
                showsDownloadedButton = state.showsDownloadedButton,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsAppDownloadAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                is AppDownloadUiAction.CopyLink -> JsAppDownloadAction("copy", action.link)
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: the same code again (an account or invite change) hands React the same array, so the SVG isn't drawn again.
    private var lastQrCode: Pair<QrCode?, Array<Boolean>>? = null

    private fun toJsModules(qrCode: QrCode?): Array<Boolean> = lastQrCode
        ?.takeIf { (kept, _) -> kept == qrCode }
        ?.second
        ?: qrCode
            ?.modules
            .orEmpty()
            .toTypedArray()
            .also { lastQrCode = qrCode to it }

    fun close() {
        viewModel.onEvent(AppDownloadUiEvent.OnCloseClicked)
    }

    fun markDownloaded() {
        viewModel.onEvent(AppDownloadUiEvent.OnDownloadedClicked)
    }

    // Why: the link under the code, which the browser opens.
    fun trackLink() {
        viewModel.onEvent(AppDownloadUiEvent.OnLinkClicked)
    }

    fun copyLink() {
        viewModel.onEvent(AppDownloadUiEvent.OnCopyLinkClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
