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
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds

private val requestTimeoutDuration = 30.seconds

// Why: httpConfig is supabase-kt's only way to reach Ktor's HttpTimeout; it is internal API, so a bump may change it.
@OptIn(SupabaseInternal::class)
internal fun createBlitzSplitSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.SUPABASE_URL,
    supabaseKey = SupabaseConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    requestTimeout = requestTimeoutDuration
    /*
     * Why: OkHttp, the Android engine, stops waiting on a silent socket after its own 10 s unless told otherwise, and
     * supabase-kt reports that as a network failure: a receipt Claude took over 10 s to read showed "No connection".
     */
    httpConfig {
        install(HttpTimeout) {
            socketTimeoutMillis = requestTimeoutDuration.inWholeMilliseconds
        }
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
