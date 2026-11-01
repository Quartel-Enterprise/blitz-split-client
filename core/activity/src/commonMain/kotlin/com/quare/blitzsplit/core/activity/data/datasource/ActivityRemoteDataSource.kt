package com.quare.blitzsplit.core.activity.data.datasource

import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadMarkDto
import com.quare.blitzsplit.core.activity.data.dto.ChargeEventDto

interface ActivityRemoteDataSource {
    // Why: every event after afterSeq that happened since `since`, oldest first.
    suspend fun fetchEventsAfter(
        afterSeq: Long,
        since: Long,
    ): List<ActivityEventDto>

    // Why: the history of groups new on the device, up to throughSeq, since `since`.
    suspend fun fetchGroupEvents(
        groupIds: List<String>,
        throughSeq: Long,
        since: Long,
    ): List<ActivityEventDto>

    // Why: the page before beforeSeq, newest first, once the feed reaches the end of what the device keeps.
    suspend fun fetchEventsBefore(
        beforeSeq: Long,
        limit: Long,
    ): List<ActivityEventDto>

    suspend fun insertCharges(events: List<ChargeEventDto>)

    suspend fun fetchReadEventIds(): List<String>

    suspend fun insertReads(reads: List<ActivityReadDto>)

    suspend fun fetchReadMark(): ActivityReadMarkDto?

    suspend fun upsertReadMark(mark: ActivityReadMarkDto)
}
