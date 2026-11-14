package com.quare.blitzsplit.shared.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveBrowserSignInReturningToProfile
import com.quare.blitzsplit.core.activity.domain.usecase.FindActivityEvent
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLink
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentTarget
import com.quare.blitzsplit.core.expenses.domain.model.BillLocation
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.expenses.domain.usecase.LocateBill
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.inappupdate.domain.usecase.RequestUpdatePromptIfNeeded
import com.quare.blitzsplit.core.legal.domain.usecase.PrefetchLegalDocuments
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.ForwardHistory
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.PushLink
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openActivity
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.ProfileLink
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.StartScreenViews
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.RefreshAppLanguage
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveAppForeground
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class AppViewModel(
    private val observeProfile: ObserveProfile,
    val navigator: Navigator,
    private val observeBrowserSignInReturningToProfile: ObserveBrowserSignInReturningToProfile,
    private val observeThemePreference: ObserveThemePreference,
    private val observeDynamicColors: ObserveDynamicColors,
    private val observeBitcoinInSats: ObserveBitcoinInSats,
    private val observeAppLanguage: ObserveAppLanguage,
    private val refreshAppLanguage: RefreshAppLanguage,
    private val linkInbox: LinkInbox,
    private val markActivityRead: MarkActivityRead,
    private val findActivityEvent: FindActivityEvent,
    private val observeGroup: ObserveGroup,
    private val observeExpense: ObserveExpense,
    private val syncNow: SyncNow,
    private val locateBill: LocateBill,
    private val noticeBillGone: NoticeBillGone,
    private val observeAppForeground: ObserveAppForeground,
    private val requestUpdatePromptIfNeeded: RequestUpdatePromptIfNeeded,
    private val startScreenViews: StartScreenViews,
    private val prefetchLegalDocuments: PrefetchLegalDocuments,
    startSync: StartSync,
) : ViewModel() {
    val uiState: StateFlow<AppUiState>
        field = MutableStateFlow(AppUiState.LOADING)

    val theme: StateFlow<ThemePreference>
        field = MutableStateFlow(ThemePreference.SYSTEM)

    // Why: Material You's scheme in place of the brand's, only where the platform draws one (Android 12 and later).
    val usesDynamicColors: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /*
     * Why: Preferências' "Bitcoin em satoshis", which every amount in bitcoin the platforms draw follows, in a list
     * as in a field.
     */
    val showsBitcoinInSats: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /*
     * Why: Profile › Language, which the Apple app's root draws its strings in at once; Android and the web apply it
     * themselves, before any screen draws.
     */
    val language: StateFlow<AppLanguage>
        field = MutableStateFlow(AppLanguage.SYSTEM)

    private val isThemeKnown = CompletableDeferred<Unit>()

    val stateWatcher: FlowWatcher<AppUiState> = FlowWatcher(uiState)
    val backStackWatcher: FlowWatcher<List<Route>> = FlowWatcher(navigator.backStack)

    // Why: true while a way out of a form with unsaved input waits for "Discard" or "Keep editing".
    val discardWatcher: FlowWatcher<Boolean> = FlowWatcher(navigator.pendingDiscard)
    val themeWatcher: FlowWatcher<ThemePreference> = FlowWatcher(theme)
    val bitcoinInSatsWatcher: FlowWatcher<Boolean> = FlowWatcher(showsBitcoinInSats)
    val languageWatcher: FlowWatcher<AppLanguage> = FlowWatcher(language)

    // Why: "Você não está neste grupo" over wherever the app is, while a comment's link can't open its bill.
    val billLinkNotice: StateFlow<BillLinkNoticeState>
        field = MutableStateFlow(BillLinkNoticeState(null))

    val billLinkNoticeWatcher: FlowWatcher<BillLinkNoticeState> = FlowWatcher(billLinkNotice)

    // Why: the Mac's mouse back and forward buttons, and Go › Back and Forward.
    private val forwardHistory = ForwardHistory(navigator, viewModelScope)

    val canGoForwardWatcher: FlowWatcher<Boolean> = FlowWatcher(forwardHistory.canGoForward)

    init {
        startSync()
        followTheme()
        followLanguage()
        resolveStartRoute()
        returnToProfileOnBrowserSignIn()
        returnToWelcomeOnceErased()
        openInviteLinks()
        offerUpdatesInFront()
        prefetchLegalTexts()
    }

    fun goBack() {
        forwardHistory.goBack()
    }

    fun goForward() {
        forwardHistory.goForward()
    }

    private fun resolveStartRoute() {
        viewModelScope.launch {
            val hasProfile = observeProfile().first() != null
            navigator.replaceAll(if (hasProfile) Route.Home else Route.Welcome)
            isThemeKnown.await()
            uiState.value = AppUiState.READY
            // Why: only once the first screen is drawn, so a link opening over it before it stayed never counts it.
            startScreenViews()
        }
    }

    /*
     * Why: the first frame waits for the chosen theme and colors, so neither flashes the system's or the brand's, and
     * for bitcoin's unit, so no amount shows in bitcoin before turning into sats.
     */
    private fun followTheme() {
        combine(
            observeThemePreference(),
            observeDynamicColors(),
            observeBitcoinInSats(),
        ) { preference, isDynamic, sats ->
            theme.value = preference
            usesDynamicColors.value = isDynamic
            showsBitcoinInSats.value = sats
            isThemeKnown.complete(Unit)
        }.launchIn(viewModelScope)
    }

    private fun followLanguage() {
        observeAppLanguage()
            .onEach { language.value = it }
            .launchIn(viewModelScope)
    }

    /*
     * Why: once the first screen is drawn, and again in each language Profile › Language switches to, so Profile's
     * footer opens the legal texts offline too. The repository asks the site at most once a day per text.
     */
    private fun prefetchLegalTexts() {
        observeAppLanguage()
            .distinctUntilChanged()
            .onEach {
                uiState.first { it == AppUiState.READY }
                prefetchLegalDocuments()
            }.launchIn(viewModelScope)
    }

    // Why: Android's own settings may change the app's language while it runs; its activity comes back in the new one.
    fun refreshLanguage() {
        refreshAppLanguage()
    }

    /*
     * Why: a browser callback that starts the app again (the web reloads the page) lands on Início,
     * so a link left from the profile goes back there to show it under way and how it ends.
     */
    private fun returnToProfileOnBrowserSignIn() {
        observeBrowserSignInReturningToProfile()
            .filterNotNull()
            .onEach {
                uiState.first { it == AppUiState.READY }
                if (navigator.backStack.value.lastOrNull() == Route.Home) navigator.navigate(Route.Profile)
            }.launchIn(viewModelScope)
    }

    /*
     * Why: each time the app comes to the front, as Bible Planner does, a newer version is offered (at most once an
     * hour) once the first screen is in place, so the start route never replaces the sheet.
     */
    private fun offerUpdatesInFront() {
        observeAppForeground()
            .filter { isForeground -> isForeground }
            .onEach {
                uiState.first { it == AppUiState.READY }
                requestUpdatePromptIfNeeded()
            }.launchIn(viewModelScope)
    }

    /*
     * Why: an invite link opens its invite over wherever the app is, once the first screen is in place, so back leaves
     * it for the groups, or the welcome without a profile. The same invite already open comes back to the top instead
     * of being pushed twice, which Navigation 3 can't hold since it keys each entry by its route.
     */
    private fun openInviteLinks() {
        linkInbox
            .observe()
            .onEach { url ->
                uiState.first { it == AppUiState.READY }
                linkInbox.take(url)
                PushLink.of(url)?.let { link -> return@onEach openPushLink(link) }
                SharedBillLink.codeOf(url)?.let { code ->
                    return@onEach openSharedBill(code, SharedBillLink.commentIdOf(url))
                }
                BillCommentLink.targetOf(url)?.let { target -> return@onEach openBillCommentLink(target) }
                ProfileLink.codeOf(url)?.let { code -> return@onEach openProfileLink(code) }
                val target = InviteLink.targetOf(url) ?: return@onEach
                val invite = Route.JoinInvite(
                    code = target.code,
                    memberCode = target.memberCode,
                    inviterCode = target.inviterCode,
                )
                // Why: the same link shared again by someone else is the same invite, whoever the sender is.
                val openIndex = navigator.backStack.value.indexOfFirst {
                    it is Route.JoinInvite && it.code == invite.code && it.memberCode == invite.memberCode
                }
                if (openIndex < 0) navigator.navigate(invite) else navigator.popTo(openIndex + 1)
            }.launchIn(viewModelScope)
    }

    // Why: a profile link opens its page as an invite does, the same one coming back to the top.
    private fun openProfileLink(code: String) {
        val openIndex = navigator.backStack.value.indexOfFirst { it is Route.ProfileLink && it.code == code }
        if (openIndex < 0) navigator.navigate(Route.ProfileLink(code)) else navigator.popTo(openIndex + 1)
    }

    // Why: a bill's link (7.3h) opens its page as an invite does, the same one coming back to the top.
    private fun openSharedBill(
        code: String,
        commentId: String?,
    ) {
        val openIndex = navigator.backStack.value.indexOfFirst { it is Route.SharedBill && it.code == code }
        val route = Route.SharedBill(code, commentId)
        when {
            openIndex < 0 -> navigator.navigate(route)

            // Why: the same page shared at a comment opens again at it, read anew.
            commentId != null -> navigator.replaceAbove(
                size = openIndex,
                route = route,
            )

            else -> navigator.popTo(openIndex + 1)
        }
    }

    /*
     * Why: a comment's link opens the bill's Conta over its group's Despesas, over Grupos, scrolled to the comment, after
     * a sync of a few seconds at most, since the comment may be newer than this device's copy; the link names no group,
     * so unlike a push notification's tap there is nothing to open before it. A deleted bill opens Despesas, which says so; a group the person isn't in, or a link this device
     * can't check offline, says so over wherever the app is. Without a profile there is no group to be in.
     */
    private fun openBillCommentLink(target: BillCommentTarget) {
        if (Route.Home !in navigator.backStack.value) return showBillLinkNotice(BillLinkNotice.NOT_IN_GROUP)
        navigator.openHomeTab(HomeTab.GROUPS)
        val tabTop = navigator.backStack.value
        viewModelScope.launch {
            withTimeoutOrNull(PUSH_LINK_SYNC_SECONDS.seconds) { syncNow() }
            if (navigator.backStack.value != tabTop) return@launch
            val location = locateBill(target.expenseId)
            // Why: the server's answer may come late, and nothing opens or speaks over a screen the person moved to.
            if (navigator.backStack.value != tabTop) return@launch
            val groupId = when (location) {
                is BillLocation.Found -> location.groupId
                is BillLocation.Deleted -> location.groupId
                BillLocation.OutOfReach -> return@launch showBillLinkNotice(BillLinkNotice.NOT_IN_GROUP)
                BillLocation.Unknown -> return@launch showBillLinkNotice(BillLinkNotice.OFFLINE)
            }
            // Why: the server says the person is in it, so a group not on the device yet comes with a later sync.
            if (observeGroup(groupId).first() == null || navigator.backStack.value != tabTop) return@launch
            if (location is BillLocation.Deleted) noticeBillGone(groupId, target.expenseId)
            navigator.navigate(Route.GroupDetail.expensesOf(groupId))
            if (location is BillLocation.Found && location.isOnDevice) {
                navigator.navigate(Route.BillDetail(groupId, target.expenseId, focusCommentId = target.commentId))
            }
        }
    }

    private fun showBillLinkNotice(notice: BillLinkNotice) {
        billLinkNotice.value = BillLinkNoticeState(notice)
    }

    // Why: "Ir para o início" on the notice, back to Início when there is one (the welcome otherwise).
    fun onBillLinkNoticeDismissed() {
        billLinkNotice.value = BillLinkNoticeState(null)
        if (Route.Home in navigator.backStack.value) navigator.openHomeTab(HomeTab.HOME)
    }

    /*
     * Why: a push notification's tap opens its tab or Atividade, and right over it what it is about with what the device
     * already has, the group and its bill, so the person lands there at once. Waiting for a sync first left Atividade
     * on screen for seconds, behind the sync the app's return to the front starts, and a tap meanwhile cancelled the
     * rest: the notification seemed to always open Atividade. What the device doesn't have yet (a group the person was
     * just added to, a new bill, a comment newer than this copy) comes with a sync of a few seconds at most, then opens
     * over what is on top while the person stayed there; offline the sync fails at once. Nothing opens for a group
     * still not on the device, nor Conta, which would close at once, for a bill still not on it. The event is read, as
     * its tap in Atividade makes it. Without a profile there is no home, and nothing opens.
     */
    private fun openPushLink(link: PushLink) {
        link.eventId?.let { eventId -> viewModelScope.launch { markActivityRead(eventId) } }
        if (Route.Home !in navigator.backStack.value) return
        link.homeTab?.let(navigator::openHomeTab) ?: navigator.openActivity()
        val routes = link.routesAbove
        if (routes.isEmpty()) return
        viewModelScope.launch {
            val openedCount = openPushRoutes(link, routes, isSynced = false) ?: return@launch
            if (openedCount == routes.size) return@launch
            val top = navigator.backStack.value
            withTimeoutOrNull(PUSH_LINK_SYNC_SECONDS.seconds) { syncNow() }
            if (navigator.backStack.value != top) return@launch
            openPushRoutes(link, routes.drop(openedCount), isSynced = true)
        }
    }

    /*
     * Why: opens the routes in order while the device has what each one shows, and says how many opened; null when the
     * person moved on meanwhile, over whom nothing opens. A comment's Conta opens at the comment, as its item in
     * Atividade does, so before the sync it waits for the comment's event too.
     */
    private suspend fun openPushRoutes(
        link: PushLink,
        routes: List<Route>,
        isSynced: Boolean,
    ): Int? {
        val top = navigator.backStack.value
        val event = link.eventId
            ?.takeIf { routes.any { route -> route is Route.BillDetail } }
            ?.let { findActivityEvent(it) }
        val focusCommentId = event?.takeUnless { it.isCommentDeleted }?.commentId
        val available = mutableListOf<Route>()
        for (route in routes) {
            val isOnDevice = when (route) {
                is Route.GroupDetail -> observeGroup(route.groupId).first() != null

                is Route.BillDetail -> observeExpense(route.expenseId).first() != null &&
                    (isSynced || event != null || !link.isComment)

                else -> true
            }
            if (!isOnDevice) break
            available += if (route is Route.BillDetail) route.copy(focusCommentId = focusCommentId) else route
        }
        if (navigator.backStack.value != top) return null
        available.forEach(navigator::navigate)
        return available.size
    }

    // Why: signing out, now or once synced, erases the profile wherever the app is, and only the welcome is left.
    private fun returnToWelcomeOnceErased() {
        observeProfile()
            .map { profile -> profile != null }
            .distinctUntilChanged()
            .dropWhile { hasProfile -> !hasProfile }
            .filter { hasProfile -> !hasProfile }
            .onEach { navigator.replaceAll(Route.Welcome) }
            .launchIn(viewModelScope)
    }

    private companion object {
        // Why: how long a tapped notification waits for a sync before opening what the device already has.
        const val PUSH_LINK_SYNC_SECONDS = 5
    }
}
