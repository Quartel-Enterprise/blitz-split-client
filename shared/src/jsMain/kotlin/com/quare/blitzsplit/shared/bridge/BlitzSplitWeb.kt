@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.shared.app.AppUiState
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.LinkInbox
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.w3c.dom.Worker
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/*
 * Why: a bill's link kept through the browser's sign in. Google's and Apple's pages take a minute or two, so a stash
 * older than the window was a sign in given up on.
 */
private const val SHARED_BILL_LEFT_TO_SIGN_IN = "blitzsplit.sharedBillLeftToSignIn"
private val sharedBillSignInWindow = 15.minutes

/*
 * Why: what React calls. Only plain types cross into TypeScript: strings, booleans, arrays,
 * callbacks and the small exported classes of this package.
 */
fun startBlitzSplit(sqliteWorker: Worker) {
    // Why: read before Auth starts, since it removes the callback's error from the fragment.
    val launchUrl = window.location.href
    startBlitzSplitKoin(extraModules = listOf(module { single<Worker> { sqliteWorker } }))
    val handleBrowserCallback = KoinPlatform.getKoin().get<HandleBrowserCallback>()
    MainScope().launch { handleBrowserCallback(launchUrl) }
    openInviteLink(launchUrl)
}

/*
 * Why: the page of a group's invite, blitzsplit.com/g/<code>, of a member's, blitzsplit.com/g/<code>/<member>, or of a
 * bill's link, blitzsplit.com/c/<code>, opens it once; the address goes back to the app's own. A bill's link left to
 * sign in (its "Entrar") opens again once the browser comes back.
 */
private fun openInviteLink(launchUrl: String) {
    val inbox = KoinPlatform.getKoin().get<LinkInbox>()
    val sharedBillLeftToSignIn = takeSharedBillLeftToSignIn()
    if (InviteLink.targetOf(launchUrl) != null || SharedBillLink.codeOf(launchUrl) != null) {
        inbox.receive(launchUrl)
        window.history.replaceState(null, "", "/")
    } else {
        sharedBillLeftToSignIn?.let(inbox::receive)
    }
}

// Why: the browser's sign in leaves the page, so a bill's link is kept, with when, for the load it comes back on.
internal fun keepSharedBillForSignIn(link: String) {
    try {
        window.sessionStorage.setItem(SHARED_BILL_LEFT_TO_SIGN_IN, "${Clock.System.now().toEpochMilliseconds()} $link")
    } catch (_: Throwable) {
        // Why: storage blocked (a private window, site data off): signing in still works, the link just doesn't reopen.
    }
}

// Why: read once on every load, so a sign in given up on (Boas-vindas' "Usar sem conta") never opens it later.
private fun takeSharedBillLeftToSignIn(): String? = try {
    window.sessionStorage
        .getItem(SHARED_BILL_LEFT_TO_SIGN_IN)
        ?.also { window.sessionStorage.removeItem(SHARED_BILL_LEFT_TO_SIGN_IN) }
        ?.split(" ", limit = 2)
        ?.takeIf { it.size == 2 }
        ?.let { (keptAt, link) ->
            val age = Clock.System.now().toEpochMilliseconds() - (keptAt.toLongOrNull() ?: return@let null)
            link.takeIf { age in 0..sharedBillSignInWindow.inWholeMilliseconds }
        }
} catch (_: Throwable) {
    null
}

class JsWatch internal constructor(
    private val onCancel: () -> Unit,
) {
    fun cancel() {
        onCancel()
    }
}

class AppPresenter {
    private val holder = ViewModelHolder(AppViewModel::class) { KoinPlatform.getKoin().get() }
    private val navigator: Navigator = holder.viewModel.navigator

    fun observeIsReady(onChange: (Boolean) -> Unit): JsWatch {
        val watch = FlowWatcher(holder.viewModel.uiState.map { it == AppUiState.READY }).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeBackStack(onChange: (Array<JsRoute>) -> Unit): JsWatch {
        val routes = navigator.backStack.map { stack -> stack.map(::JsRoute).toTypedArray() }
        val watch = FlowWatcher(routes).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "system", "light" or "dark"; the page follows the system's scheme only for "system".
    fun observeTheme(onChange: (String) -> Unit): JsWatch {
        val watch = FlowWatcher(holder.viewModel.theme.map { it.code }).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun navigateBack() {
        navigator.navigateBack()
    }

    fun popTo(size: Int) {
        navigator.popTo(size)
    }

    fun dispose() {
        holder.clear()
    }
}
