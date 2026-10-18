package com.quare.blitzsplit.core.provider.supabase

import io.github.jan.supabase.auth.AuthConfig

/*
 * Why: the native apps receive the OAuth callback through the borarachar:// deep link, while the
 * browser comes back to the page itself.
 */
internal expect fun AuthConfig.configurePlatform()
