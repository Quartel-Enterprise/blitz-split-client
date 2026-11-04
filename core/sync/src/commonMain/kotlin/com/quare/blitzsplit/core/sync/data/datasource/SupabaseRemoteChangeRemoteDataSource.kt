package com.quare.blitzsplit.core.sync.data.datasource

import com.quare.blitzsplit.core.sync.data.dto.RemoteChangeDto
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/*
 * Why: one channel for the user, with no filter: the server's row level security decides which events and profiles
 * each subscriber hears of (blitz-split-api's 20261126120000_publish_realtime_changes). The channel leaves when the
 * collection ends, so nothing listens once the app leaves the front or goes offline.
 */
class SupabaseRemoteChangeRemoteDataSource(
    private val realtime: Realtime,
) : RemoteChangeRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeChanges(): Flow<RemoteChangeDto> = channelFlow {
        val channel = realtime.channel(CHANNEL_ID)
        // Why: supabase-kt only takes the subscriptions declared before the channel joins.
        val events = channel.postgresChangeFlow<PostgresAction.Insert>(schema = SCHEMA) { table = EVENTS }
        val profiles = channel.postgresChangeFlow<PostgresAction.Update>(schema = SCHEMA) { table = PROFILES }
        try {
            // Why: undispatched, so each listens before the channel joins and the first change can't slip by.
            launch(start = CoroutineStart.UNDISPATCHED) {
                events.collect { action -> decode<RemoteChangeDto.ActivityInsert>(action.record)?.let { send(it) } }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                profiles.collect { action -> decode<RemoteChangeDto.ProfileUpdate>(action.record)?.let { send(it) } }
            }
            // Why: a rejoin after the socket dropped goes through SUBSCRIBING again, so each join counts once.
            launch(start = CoroutineStart.UNDISPATCHED) {
                channel.status
                    .filter { it == RealtimeChannel.Status.SUBSCRIBED }
                    .collect { send(RemoteChangeDto.Joined) }
            }
            channel.subscribe()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { realtime.removeChannel(channel) }
        }
    }

    // Why: a row this version can't read is skipped; the next sync brings whatever it was about.
    private inline fun <reified T> decode(record: JsonObject): T? = try {
        json.decodeFromJsonElement<T>(record)
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val CHANNEL_ID = "changes"
        const val SCHEMA = "public"
        const val EVENTS = "activity_events"
        const val PROFILES = "profiles"
    }
}
