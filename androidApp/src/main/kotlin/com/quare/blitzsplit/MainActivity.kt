package com.quare.blitzsplit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.navigation.AppNavDisplay
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val supabase: SupabaseClient by inject()
    private val handleBrowserCallback: HandleBrowserCallback by inject()

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

    private fun handleAuthCallback(intent: Intent) {
        supabase.handleDeeplinks(intent)
        val url = intent.dataString ?: return
        lifecycleScope.launch { handleBrowserCallback(url) }
    }
}
