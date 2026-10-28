package com.quare.blitzsplit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.navigation.AppNavDisplay
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val supabase: SupabaseClient by inject()
    private val handleBrowserCallback: HandleBrowserCallback by inject()
    private val handleAppPaused: HandleAppPaused by inject()
    private val handleAppResumed: HandleAppResumed by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Why: a recreated activity keeps the intent that launched it, an old callback by now.
        if (savedInstanceState == null) handleAuthCallback(intent)
        setContent {
            BlitzSplitTheme {
                AppNavDisplay()
            }
        }
    }

    // Why: singleTask, so the browser sign in comes back here as borarachar://auth-callback.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
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

    private fun handleAuthCallback(intent: Intent) {
        supabase.handleDeeplinks(intent)
        val url = intent.dataString ?: return
        lifecycleScope.launch { handleBrowserCallback(url) }
    }
}
