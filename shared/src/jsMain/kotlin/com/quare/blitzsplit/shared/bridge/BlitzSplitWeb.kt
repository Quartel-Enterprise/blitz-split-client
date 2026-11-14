@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.PushLink
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.core.profile.domain.model.ProfileLink
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.core.sync.domain.usecase.ReportAppForeground
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.shared.app.AppUiState
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.BillLinkNotice
import com.quare.blitzsplit.shared.app.LinkInbox
import com.quare.blitzsplit.shared.app.sharedBillSignInWindow
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.w3c.dom.Worker
import kotlin.time.Clock

/*
 * Why: a bill's link kept through the browser's sign in. A stash older than the window (sharedBillSignInWindow, the
 * apps' own too) was a sign in given up on.
 */
private const val SHARED_BILL_LEFT_TO_SIGN_IN = "blitzsplit.sharedBillLeftToSignIn"
private const val PROFILE_LEFT_FOR_LANGUAGE = "blitzsplit.profileLeftForLanguage"

/*
 * Why: what React calls. Only plain types cross into TypeScript: strings, booleans, arrays,
 * callbacks and the small exported classes of this package.
 */
fun startBlitzSplit(
    sqliteWorker: Worker,
    languageTag: String,
) {
    // Why: read before Auth starts, since it removes the callback's error from the fragment.
    val launchUrl = window.location.href
    // Why: the UI's language is picked once at startup, so its separator is read once too.
    val decimalSeparator = decimalSeparatorOf(languageTag)
    startBlitzSplitKoin(
        extraModules = listOf(
            module {
                single<Worker> { sqliteWorker }
                single { DeviceDecimalSeparator { decimalSeparator } }
            },
        ),
    )
    val handleBrowserCallback = KoinPlatform.getKoin().get<HandleBrowserCallback>()
    MainScope().launch { handleBrowserCallback(launchUrl) }
    openInviteLink(launchUrl)
    forgetSharedBillOnceProfileMade()
    reopenProfileAfterLanguage()
}

/*
 * Why: the separator Intl writes in 1.5 for the language the UI speaks, not navigator.language: Chrome gives that its
 * own menus' language, which may be English while the pages and the app are in Portuguese.
 */
private fun decimalSeparatorOf(languageTag: String): Char = try {
    (js("Intl").NumberFormat(languageTag).format(1.5) as? String)?.getOrNull(1) ?: '.'
} catch (unsupported: Throwable) {
    '.'
}

/*
 * Why: the page of a group's invite, blitzsplit.com/g/<code>, of a member's, blitzsplit.com/g/<code>/<member>, of a
 * bill's link, blitzsplit.com/c/<code>, of a comment's, blitzsplit.com/b/<bill>?comentario=<id>, of a profile's,
 * blitzsplit.com/u/<code>, or of a notification
 * tapped with no tab open, blitzsplit.com/n?…, opens it once; the address goes back to the app's own. A bill's link
 * left to sign in (its "Entrar") opens again once the browser comes back.
 */
private fun openInviteLink(launchUrl: String) {
    val inbox = KoinPlatform.getKoin().get<LinkInbox>()
    val sharedBillLeftToSignIn = takeSharedBillLeftToSignIn()
    val isLink = InviteLink.targetOf(launchUrl) != null ||
        SharedBillLink.codeOf(launchUrl) != null ||
        BillCommentLink.targetOf(launchUrl) != null ||
        ProfileLink.codeOf(launchUrl) != null ||
        PushLink.of(launchUrl) != null
    if (isLink) {
        inbox.receive(launchUrl)
        window.history.replaceState(null, "", "/")
    } else {
        sharedBillLeftToSignIn?.let(inbox::receive)
    }
}

/*
 * Why: the browser's Web Push subscription, for Atividade's push notifications: its endpoint is the device's token,
 * and its keys are what each message is encrypted for.
 */
fun setPushDevice(
    endpoint: String,
    p256dh: String,
    auth: String,
) {
    KoinPlatform.getKoin().get<SetPushDevice>().invoke(
        PushDevice(
            token = endpoint,
            platform = PushPlatform.WEB,
            apnsEnvironment = null,
            webP256dh = p256dh,
            webAuth = auth,
        ),
    )
}

// Why: the currencies this version lists, so the web writes theirs by symbol and any other code by its raw code.
fun getKnownCurrencyCodes(): Array<String> = GroupCurrency.entries.toCurrencyCodes()

// Why: real time listens only while the page is visible (document.visibilityState).
fun setAppForeground(isForeground: Boolean) {
    KoinPlatform.getKoin().get<ReportAppForeground>().invoke(isForeground)
}

// Why: a notification tapped while a tab is open, which the service worker hands to it instead of opening another.
fun openNotificationLink(url: String) {
    if (PushLink.of(url) != null) KoinPlatform.getKoin().get<LinkInbox>().receive(url)
}

/*
 * Why: Boas-vindas without the browser ("Usar sem conta") makes the profile on this page, and the app itself goes back
 * to the bill (AppViewModel). The link kept for a browser's sign in is dropped then, so a later load never opens it.
 */
private fun forgetSharedBillOnceProfileMade() {
    val observeProfile = KoinPlatform.getKoin().get<ObserveProfile>()
    observeProfile()
        .map { profile -> profile != null }
        .distinctUntilChanged()
        .dropWhile { hasProfile -> hasProfile }
        .filter { hasProfile -> hasProfile }
        .onEach { takeSharedBillLeftToSignIn() }
        .launchIn(MainScope())
}

/*
 * Why: Profile › Language reloads the page in the language picked, which comes back to the profile rather than Início:
 * once Início is in place, as the start route puts it, the profile opens over it as before the reload.
 */
private fun reopenProfileAfterLanguage() {
    val wasInProfile = try {
        window.sessionStorage.getItem(PROFILE_LEFT_FOR_LANGUAGE)?.also {
            window.sessionStorage.removeItem(PROFILE_LEFT_FOR_LANGUAGE)
        } != null
    } catch (_: Throwable) {
        false
    }
    if (!wasInProfile) return
    val navigator = KoinPlatform.getKoin().get<Navigator>()
    MainScope().launch {
        // Why: only the start route's own Início, never one a later back reaches after a link opened over it.
        val start = navigator.backStack.first { it.firstOrNull() == Route.Home }
        if (start.lastOrNull() == Route.Home) navigator.navigate(Route.Profile)
    }
}

// Why: kept before Profile reloads the page for a language picked.
internal fun keepProfileForLanguage() {
    try {
        window.sessionStorage.setItem(PROFILE_LEFT_FOR_LANGUAGE, "1")
    } catch (_: Throwable) {
        // Why: storage blocked: the page still reloads in the language picked, on Início.
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

// Why: read once, so a sign in given up on never opens it on a later load.
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

// Why: "not-in-group" ("Você não está neste grupo") or "offline" ("Sem internet"); null for no notice.
class JsBillLinkNotice(
    val notice: String?,
)

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

    // Why: why a comment's link opened nothing, over wherever the app is.
    fun observeBillLinkNotice(onChange: (JsBillLinkNotice) -> Unit): JsWatch {
        val notices = holder.viewModel.billLinkNotice.map { state ->
            JsBillLinkNotice(
                notice = when (state.notice) {
                    BillLinkNotice.NOT_IN_GROUP -> "not-in-group"
                    BillLinkNotice.OFFLINE -> "offline"
                    null -> null
                },
            )
        }
        val watch = FlowWatcher(notices).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "Ir para o início" on the notice, or the notice closed some other way.
    fun dismissBillLinkNotice() {
        holder.viewModel.onBillLinkNoticeDismissed()
    }

    // Why: Preferências' "Bitcoin em satoshis", which every bitcoin amount the page writes follows.
    fun observeBitcoinInSats(onChange: (Boolean) -> Unit): JsWatch {
        val watch = FlowWatcher(holder.viewModel.showsBitcoinInSats).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun navigateBack() {
        navigator.navigateBack()
    }

    fun popTo(size: Int) {
        navigator.popTo(size)
    }

    // Why: "Descartar alterações?", asked while a way out of a form with unsaved input waits for the answer.
    fun observeDiscardPending(onChange: (Boolean) -> Unit): JsWatch {
        val watch = FlowWatcher(navigator.pendingDiscard).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: the page warns before it closes while a form holds input that leaving would lose.
    fun observeHasUnsavedInput(onChange: (Boolean) -> Unit): JsWatch {
        val watch = FlowWatcher(navigator.hasUnsavedInput).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: the browser's back button already moved the history, so it must go forward again when the stack held.
    fun isDiscardPending(): Boolean = navigator.pendingDiscard.value

    fun confirmDiscard() {
        navigator.confirmDiscard()
    }

    fun cancelDiscard() {
        navigator.cancelDiscard()
    }

    fun dispose() {
        holder.clear()
    }
}
