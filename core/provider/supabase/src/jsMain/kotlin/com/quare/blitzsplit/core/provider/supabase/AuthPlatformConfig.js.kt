package com.quare.blitzsplit.core.provider.supabase

import io.github.jan.supabase.auth.AuthConfig

internal actual fun AuthConfig.configurePlatform() {
    /*
     * Why: the browser returns to the page that started the OAuth flow, and Auth imports the
     * session from its URL on start, so there is no deep link to configure.
     */
    autoLoadFromStorage = true
}
