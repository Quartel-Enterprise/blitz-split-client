package com.quare.blitzsplit.wear

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import com.quare.blitzsplit.wear.ui.WearRoot

class MainActivity : ComponentActivity() {
    // Why: Profile › Language's pick before Android 13, as every screen of the app follows it.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearRoot() }
    }
}
