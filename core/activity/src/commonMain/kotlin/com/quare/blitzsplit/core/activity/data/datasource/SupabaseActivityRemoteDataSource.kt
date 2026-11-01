package com.quare.blitzsplit.core.activity.data.datasource

import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadMarkDto
import com.quare.blitzsplit.core.activity.data.dto.ChargeEventDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseActivityRemoteDataSource(
    private val postgrest: Postgrest,
) : ActivityRemoteDataSource {
    /*
     * Why: page by page after the last seq of the page before, until one comes back short. New events only ever take a
     * higher seq, so nothing shifts between pages.
     */
    override suspend fun fetchEventsAfter(
        afterSeq: Long,
        since: Long,
    ): List<ActivityEventDto> {
        val events = mutableListOf<ActivityEventDto>()
        do {
            val cursor = events.lastOrNull()?.seq ?: afterSeq
            val page = postgrest
                .from(EVENTS)
                .select {
                    filter {
                        gt("seq", cursor)
                        gte("occurred_at", since)
                    }
                    order(
                        column = "seq",
                        order = Order.ASCENDING,
                    )
                    limit(PAGE_SIZE)
                }.decodeList<ActivityEventDto>()
            events += page
        } while (page.size.toLong() == PAGE_SIZE)
        return events
    }

    // Why: a few groups per request, so the filter in the URL stays short.
    override suspend fun fetchGroupEvents(
        groupIds: List<String>,
        throughSeq: Long,
        since: Long,
    ): List<ActivityEventDto> = groupIds.chunked(GROUPS_PER_REQUEST).flatMap { chunk ->
        val events = mutableListOf<ActivityEventDto>()
        do {
            val cursor = events.lastOrNull()?.seq ?: 0L
            val page = postgrest
                .from(EVENTS)
                .select {
                    filter {
                        isIn("group_id", chunk)
                        gt("seq", cursor)
                        lte("seq", throughSeq)
                        gte("occurred_at", since)
                    }
                    order(
                        column = "seq",
                        order = Order.ASCENDING,
                    )
                    limit(PAGE_SIZE)
                }.decodeList<ActivityEventDto>()
            events += page
        } while (page.size.toLong() == PAGE_SIZE)
        events
    }

    override suspend fun fetchEventsBefore(
        beforeSeq: Long,
        limit: Long,
    ): List<ActivityEventDto> = postgrest
        .from(EVENTS)
        .select {
            filter { lt("seq", beforeSeq) }
            order(
                column = "seq",
                order = Order.DESCENDING,
            )
            limit(limit)
        }.decodeList<ActivityEventDto>()

    // Why: a charge that went up once and whose answer was lost goes up again as the same event, which stays one.
    override suspend fun insertCharges(events: List<ChargeEventDto>) {
        postgrest.from(EVENTS).upsert(events) { ignoreDuplicates = true }
    }

    override suspend fun fetchReadEventIds(): List<String> = postgrest
        .from(READS)
        .select()
        .decodeList<ActivityReadDto>()
        .map { it.eventId }

    override suspend fun insertReads(reads: List<ActivityReadDto>) {
        postgrest.from(READS).upsert(reads) {
            onConflict = "user_id,event_id"
            ignoreDuplicates = true
        }
    }

    override suspend fun fetchReadMark(): ActivityReadMarkDto? = postgrest
        .from(READ_MARKS)
        .select()
        .decodeList<ActivityReadMarkDto>()
        .firstOrNull()

    // Why: the server keeps the further mark, so an older one from another device never moves it back.
    override suspend fun upsertReadMark(mark: ActivityReadMarkDto) {
        postgrest.from(READ_MARKS).upsert(mark) { onConflict = "user_id" }
    }

    private companion object {
        const val EVENTS = "activity_events"
        const val READS = "activity_reads"
        const val READ_MARKS = "activity_read_marks"
        const val PAGE_SIZE = 1_000L
        const val GROUPS_PER_REQUEST = 50
    }
}
