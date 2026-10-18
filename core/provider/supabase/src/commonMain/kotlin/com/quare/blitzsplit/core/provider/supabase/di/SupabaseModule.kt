package com.quare.blitzsplit.core.provider.supabase.di

import com.quare.blitzsplit.core.provider.supabase.createBlitzSplitSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import org.koin.dsl.module

val supabaseModule = module {
    single<SupabaseClient> { createBlitzSplitSupabaseClient() }
    single<Auth> { get<SupabaseClient>().auth }
    single<Postgrest> { get<SupabaseClient>().postgrest }
    single<Functions> { get<SupabaseClient>().functions }
}
