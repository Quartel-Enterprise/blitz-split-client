package com.quare.blitzsplit.core.provider.supabase

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType

internal actual fun AuthConfig.configurePlatform() {
    flowType = FlowType.PKCE
    scheme = AUTH_CALLBACK_SCHEME
    host = AUTH_CALLBACK_HOST
}
