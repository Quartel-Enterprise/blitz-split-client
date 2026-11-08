package com.quare.blitzsplit.core.provider.supabase

import com.quare.blitzsplit.core.provider.supabase.generated.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

// Why: httpConfig is the only way to add a Ktor plugin to supabase-kt's client, and it is marked internal.
@OptIn(SupabaseInternal::class)
internal fun createBlitzSplitSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.SUPABASE_URL,
    supabaseKey = SupabaseConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    requestTimeout = 30.seconds
    httpConfig {
        install(
            createUserTableAccess(
                supabaseUrl = SupabaseConfig.SUPABASE_URL,
                publishableKey = SupabaseConfig.SUPABASE_PUBLISHABLE_KEY,
            ),
        )
    }
    install(Auth) {
        configurePlatform()
    }
    install(Postgrest)
    install(Functions)
    install(Storage)
    // Why: a dropped socket shows sooner and comes back sooner than the defaults (15 s and 7 s), as in Bible Planner.
    install(Realtime) {
        heartbeatInterval = 10.seconds
        reconnectDelay = 3.seconds
    }
}
