package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.PushLink
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.core.profile.domain.model.ApnsEnvironment
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.core.sync.di.lastSyncRecordingModule
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.watch.domain.usecase.EndWatchSessionFromPhone
import com.quare.blitzsplit.feature.watchbalances.presentation.navigation.OpenWatchGroup
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchBalancesViewModel
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchGroupViewModel
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchPairingViewModel
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchRootViewModel
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import com.quare.blitzsplit.shared.push.isSyncedWithin
import com.quare.blitzsplit.shared.storescreenshots.startSeededForStoreScreenshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalNativeApi
import kotlin.time.Duration.Companion.seconds

/*
 * Why: what the Apple Watch app calls besides BlitzSplitApple (which writes the complication's snapshot and reports
 * the app in front): starting the shared code as a watch, the watch's screens, its back stack, and what the iPhone
 * and the watch's own pushes ask of it. Only the watchOS framework has it.
 */
object BlitzSplitWatch : KoinComponent {
    private val scope = MainScope() + Dispatchers.Main.immediate

    // Why: as the phone's start, plus the time of each sync that went through, the balances' "Updated at 14:32".
    fun start() {
        startBlitzSplitKoin(extraModules = listOf(lastSyncRecordingModule))
    }

    /*
     * Why: the README's image (scripts/readme-hero): a debug build launched with `-StoreScreenshotsLanguage` holds the
     * sample trip, as the iPhone's startForStoreScreenshots does, in a Room of its own and with no sync, so the watch
     * opens on Balances with the trip's groups. A release framework never seeds. The store listing's capture hands over
     * the folder of the image bank's photos (faces/, groups/) in assetsDirectory, so the people have their faces.
     */
    @OptIn(ExperimentalNativeApi::class)
    fun startForStoreScreenshots(
        language: String,
        assetsDirectory: String?,
    ) {
        if (!Platform.isDebugBinary) {
            start()
            return
        }
        startSeededForStoreScreenshots(
            language = language,
            theme = ThemePreference.DARK,
            assetsDirectory = assetsDirectory,
            extraModules = listOf(lastSyncRecordingModule),
        )
    }

    // Why: the watch's NavigationStack draws the shared back stack, as the phone's does.
    fun createBackStackWatcher(): FlowWatcher<List<Route>> = FlowWatcher(get<Navigator>().backStack)

    val backStack: List<Route>
        get() = get<Navigator>().backStack.value

    // Why: the system's back (the toolbar's chevron, the swipe) popped the NavigationStack to size entries.
    fun popTo(size: Int) {
        get<Navigator>().popTo(size)
    }

    /*
     * Why: the complications and the Smart Stack card of a watch with no account (never connected, or signed out) show
     * the app's icon alone, never "All settled" (docs/product/watch.md#glanceable-surfaces).
     */
    fun createHasAccountWatcher(): FlowWatcher<Boolean> =
        FlowWatcher(get<ObserveProfile>()().map { it != null }.distinctUntilChanged())

    fun createWatchRootViewModel(): ViewModelHolder<WatchRootViewModel> =
        ViewModelHolder(WatchRootViewModel::class) { get() }

    fun createWatchPairingViewModel(): ViewModelHolder<WatchPairingViewModel> =
        ViewModelHolder(WatchPairingViewModel::class) { get() }

    fun createWatchBalancesViewModel(): ViewModelHolder<WatchBalancesViewModel> =
        ViewModelHolder(WatchBalancesViewModel::class) { get() }

    fun createWatchGroupViewModel(route: Route.WatchGroup): ViewModelHolder<WatchGroupViewModel> =
        ViewModelHolder(WatchGroupViewModel::class) { get { parametersOf(route) } }

    /*
     * Why: the iPhone signed out of userId (WatchConnectivity's user info), so the watch signs out and erases its copy
     * once what it queued went up (15.13); another account's sign out leaves it as it is.
     */
    fun endSessionFromPhone(userId: String) {
        val endSession = get<EndWatchSessionFromPhone>()
        scope.launch { endSession(userId) }
    }

    /*
     * Why: the watch's own APNs token (15.7), as hex, on every launch: the server sends the watch's pushes with the
     * watch app's bundle id as their topic, apart from the iPhone's. A debug build's token is the sandbox's.
     */
    fun setPushToken(
        token: String,
        isSandbox: Boolean,
    ) {
        get<SetPushDevice>().invoke(
            PushDevice(
                token = token,
                platform = PushPlatform.WATCHOS,
                apnsEnvironment = if (isSandbox) ApnsEnvironment.DEVELOPMENT else ApnsEnvironment.PRODUCTION,
                webP256dh = null,
                webAuth = null,
            ),
        )
    }

    /*
     * Why: "Received" on "Elisa marked that she paid you" (15.7): the payment is already recorded by whoever paid, so
     * the watch takes it as seen, the activity read (Pierre's call, November 2026). onDone lets the system's handler
     * end.
     */
    fun handleReceived(
        link: String,
        onDone: () -> Unit,
    ) {
        val eventId = PushLink.of(link)?.eventId
        if (eventId == null) {
            onDone()
            return
        }
        val markActivityRead = get<MarkActivityRead>()
        scope.launch {
            markActivityRead(eventId)
            onDone()
        }
    }

    // Why: a push's tap or "View group" opens its group on the watch, in the currency it is open in.
    fun openPush(link: String) {
        val openWatchGroup = get<OpenWatchGroup>()
        scope.launch { openWatchGroup(PushLink.of(link)?.groupId) }
    }

    /*
     * Why: watchOS's background refresh, a few times an hour at most, so the complication and the Smart Stack card
     * follow the other devices' changes while the app isn't open. onDone ends the background task, within the few
     * seconds watchOS gives it even when the network hangs: a refresh that runs over is killed and costs budget.
     */
    fun syncInBackground(onDone: () -> Unit) {
        val syncNow = get<SyncNow>()
        scope.launch {
            syncNow.isSyncedWithin(backgroundSyncLimit)
            onDone()
        }
    }

    private val backgroundSyncLimit = 10.seconds
}
