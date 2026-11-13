package com.quare.blitzsplit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.quare.blitzsplit.android.designsystem.format.FormattersRoot
import com.quare.blitzsplit.android.designsystem.format.LocalShowsBitcoinInSats
import com.quare.blitzsplit.android.designsystem.layout.FoldLayoutRoot
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.feature.inappupdate.InAppUpdateDownloadOverlay
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.navigation.AppNavDisplay
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.LinkInbox
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    private val supabase: SupabaseClient by inject()
    private val handleBrowserCallback: HandleBrowserCallback by inject()
    private val handleAppPaused: HandleAppPaused by inject()
    private val handleAppResumed: HandleAppResumed by inject()
    private val linkInbox: LinkInbox by inject()

    // Why: before Android 13 the language Profile picked applies here, so the first frame is already in it.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        /*
         * Why: a recreated activity keeps the intent that launched it, an old callback or link by now, and so does
         * one Recents starts again after back closed it.
         */
        val isFromRecents = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !isFromRecents) handleLink(intent)
        setContent {
            val appViewModel: AppViewModel = koinViewModel()
            /*
             * Why: the system's settings may have changed the app's language while it was away: a new language opens
             * the activity again in it, while the same one it already drew in (System to that language) only resumes.
             */
            LifecycleResumeEffect(appViewModel) {
                appViewModel.refreshLanguage()
                onPauseOrDispose {}
            }
            val theme by appViewModel.theme.collectAsStateWithLifecycle()
            val usesDynamicColors by appViewModel.usesDynamicColors.collectAsStateWithLifecycle()
            val showsBitcoinInSats by appViewModel.showsBitcoinInSats.collectAsStateWithLifecycle()
            val isDark = when (theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            // Why: the status and navigation bar icons follow the app's theme, not only the system's.
            LaunchedEffect(isDark) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle,
                )
            }
            BlitzSplitTheme(
                isDark = isDark,
                usesDynamicColors = usesDynamicColors,
            ) {
                CompositionLocalProvider(LocalShowsBitcoinInSats provides showsBitcoinInSats) {
                    FormattersRoot {
                        FoldLayoutRoot {
                            AppNavDisplay(appViewModel = appViewModel)
                            BillLinkNoticeDialog(appViewModel = appViewModel)
                            DiscardChangesDialog(appViewModel = appViewModel)
                            InAppUpdateDownloadOverlay()
                        }
                    }
                }
            }
        }
    }

    // Why: singleTask, so the browser sign in comes back here as borarachar://auth-callback, and an invite link too.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLink(intent)
    }

    // Why: the browser sign in opens over this activity, which pauses it.
    override fun onPause() {
        super.onPause()
        handleAppPaused()
    }

    // Why: Android calls it after onNewIntent, so a browser sign in that came back with its callback isn't left open.
    override fun onResume() {
        super.onResume()
        handleAppResumed()
    }

    // Why: each handler takes only the links it knows: the sign in's callback, an invite, a notification's tap.
    private fun handleLink(intent: Intent) {
        supabase.handleDeeplinks(intent)
        val url = intent.dataString ?: return
        lifecycleScope.launch { handleBrowserCallback(url) }
        linkInbox.receive(url)
    }
}
