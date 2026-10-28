@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.shared.app.AppUiState
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.w3c.dom.Worker

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

    fun observeBackStack(onChange: (Array<String>) -> Unit): JsWatch {
        val routes = navigator.backStack.map { stack -> stack.map(::toRouteName).toTypedArray() }
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
