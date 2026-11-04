package com.quare.blitzsplit.core.sync.data.datasource

import com.quare.blitzsplit.core.sync.data.dto.RemoteChangeDto
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
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
            launch(start = CoroutineStart.UNDISPATCHED) { announceJoins(channel) }
            channel.subscribe()
            failOnceRefused(channel)
        } finally {
            withContext(NonCancellable) { realtime.removeChannel(channel) }
        }
    }

    /*
     * Why: each join is a moment to catch up, the channel's first and every one after the socket dropped. The socket's
     * coming back counts too: the channel's rejoin may pass through SUBSCRIBING too fast for a StateFlow collector to
     * see, while the socket stays down at least the reconnect delay.
     */
    private suspend fun ProducerScope<RemoteChangeDto>.announceJoins(channel: RealtimeChannel) {
        var hasJoined = false
        launch(start = CoroutineStart.UNDISPATCHED) {
            var wasDown = false
            realtime.status.collect { status ->
                if (status != Realtime.Status.CONNECTED) {
                    wasDown = true
                } else if (wasDown) {
                    wasDown = false
                    if (hasJoined) send(RemoteChangeDto.Joined)
                }
            }
        }
        channel.status
            .filter { it == RealtimeChannel.Status.SUBSCRIBED }
            .collect {
                hasJoined = true
                send(RemoteChangeDto.Joined)
            }
    }

    /*
     * Why: a channel the server refuses or closes falls back to UNSUBSCRIBED, and supabase-kt stops rejoining after a
     * few tries (2 s apart); one left there longer fails, so RemoteChangeListener listens again later. A socket that
     * drops and comes back rejoins well within it.
     */
    private suspend fun failOnceRefused(channel: RealtimeChannel) {
        channel.status.collectLatest { status ->
            if (status == RealtimeChannel.Status.UNSUBSCRIBED) {
                delay(REFUSED_AFTER_MILLIS)
                error("The real time channel was refused or closed")
            }
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
        const val REFUSED_AFTER_MILLIS = 20_000L
    }
}
