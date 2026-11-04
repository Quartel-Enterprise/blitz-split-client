package com.quare.blitzsplit.core.provider.analytics.screen

import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds

/*
 * Why: a route only counts once it stays on top for a moment, so one gone at once never does, and neither does the
 * screen under it coming back: the Mac pops Perfil and Mapa de categorias right away to open them in Ajustes, and a
 * link replaces Início with the group it opens.
 */
internal val screenStay = 500.milliseconds

/*
 * Why: a screen counts each time it comes on top and stays, back to it included, and once per route: another group's
 * Despesas is another view, the same one drawn again is not. One for the app's whole life, following the one back
 * stack every window of the Mac shares, so it starts once however many AppViewModels ask.
 */
@OptIn(FlowPreview::class, ExperimentalAtomicApi::class)
internal class ScreenViewTracker(
    private val navigator: Navigator,
    private val trackEvent: TrackEvent,
    private val scope: CoroutineScope,
) {
    private val isStarted = AtomicBoolean(false)

    fun start() {
        if (!isStarted.compareAndSet(
                expectedValue = false,
                newValue = true,
            )
        ) {
            return
        }
        navigator.backStack
            .map { routes -> routes.lastOrNull() }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(screenStay)
            .distinctUntilChanged()
            .onEach { route ->
                val screenView = route.toScreenView()
                trackEvent(
                    name = SCREEN_VIEW,
                    params = screenView.params + mapOf(SCREEN_NAME to screenView.name, SCREEN_CLASS to screenView.name),
                )
            }.launchIn(scope)
    }

    private companion object {
        // Why: GA4's own event and params for a screen, which its reports read.
        const val SCREEN_VIEW = "screen_view"
        const val SCREEN_NAME = "screen_name"
        const val SCREEN_CLASS = "screen_class"
    }
}
