package com.quare.blitzsplit.core.activity.data

import com.quare.blitzsplit.core.activity.data.datasource.ActivityLocalDataSource
import com.quare.blitzsplit.core.activity.data.datasource.ActivityRemoteDataSource
import com.quare.blitzsplit.core.activity.data.dto.ActivityEventDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadDto
import com.quare.blitzsplit.core.activity.data.dto.ActivityReadMarkDto
import com.quare.blitzsplit.core.activity.data.dto.ChargeEventDto
import com.quare.blitzsplit.core.activity.data.mapper.ActivityMapperImpl
import com.quare.blitzsplit.core.activity.data.repository.ActivityRepositoryImpl
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityStateEntity
import com.quare.blitzsplit.core.provider.room.entity.QueuedBillRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedGroupRow
import com.quare.blitzsplit.core.provider.room.entity.QueuedPaymentRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ActivityRepositoryImplTest {
    private lateinit var local: FakeActivityLocalDataSource
    private lateinit var remote: FakeActivityRemoteDataSource
    private lateinit var repository: ActivityRepositoryImpl

    private val now = 100.days.inWholeMilliseconds
    private val ninetyDaysAgo = now - 90.days.inWholeMilliseconds

    @Test
    fun `GIVEN events on the device and one of a kind unknown WHEN observing THEN shows the known ones`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(
                entity(
                    id = "bill",
                    seq = 2L,
                ),
                entity(
                    id = "comment",
                    seq = 1L,
                    kind = "comment_added",
                ),
            ),
        )

        // When
        val events = repository.observeEvents().first()

        // Then
        assertEquals(listOf("bill"), events.map { it.id })
    }

    @Test
    fun `GIVEN pending bills payments and groups WHEN observing the queue THEN lists them the latest saved first`() =
        runTest {
            // Given
            prepareScenario()
            local.queuedBills.value = listOf(billRow(updatedAt = 5L))
            local.queuedPayments.value = listOf(paymentRow(updatedAt = 9L))
            local.queuedGroups.value = listOf(groupRow(updatedAt = 7L))

            // When
            val queued = repository.observeQueued().first()

            // Then
            assertEquals(
                listOf(ActivityKind.PAYMENT_RECORDED, ActivityKind.GROUP_CREATED, ActivityKind.BILL_CREATED),
                queued.map { it.kind },
            )
        }

    @Test
    fun `GIVEN nothing synced yet WHEN observing the read state THEN nothing is read and the feed never came down`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val readState = repository.observeReadState().first()

            // Then
            assertEquals(ActivityReadState.initial, readState)
        }

    @Test
    fun `GIVEN a mark and opened events WHEN observing the read state THEN reads both`() = runTest {
        // Given
        prepareScenario(
            storedState = state(
                readThroughSeq = 10L,
                hasDownloaded = true,
            ),
        )
        local.reads.value = mapOf("event-1" to false, "event-2" to true)

        // When
        val readState = repository.observeReadState().first()

        // Then
        assertEquals(
            ActivityReadState(
                readThroughSeq = 10L,
                readEventIds = setOf("event-1", "event-2"),
                hasDownloaded = true,
            ),
            readState,
        )
    }

    @Test
    fun `GIVEN bills and groups on the device WHEN observing the targets THEN gives their ids`() = runTest {
        // Given
        prepareScenario()
        local.liveBillIds.value = listOf("bill-1", "bill-2")
        local.liveGroupIds.value = listOf(GROUP_ID)

        // When
        val targets = repository.observeTargets().first()

        // Then
        assertEquals(
            ActivityTargets(
                billIds = setOf("bill-1", "bill-2"),
                groupIds = setOf(GROUP_ID),
            ),
            targets,
        )
    }

    @Test
    fun `GIVEN an event WHEN opening it THEN it is read and waits to go up`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.saveRead("event-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mapOf("event-1" to true), local.reads.value)
    }

    @Test
    fun `GIVEN the device fails to save WHEN opening an event THEN returns the failure`() = runTest {
        // Given
        prepareScenario()
        local.writeError = IllegalStateException("database closed")

        // When
        val result = repository.saveRead("event-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN events WHEN marking all read THEN saves the mark with its time`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.saveReadThrough(
            throughSeq = 12L,
            now = 5_000L,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(12L to 5_000L), local.markAllCalls)
    }

    @Test
    fun `GIVEN charges in two groups WHEN recording them THEN saves one pending charge each from the user`() = runTest {
        // Given
        prepareScenario()
        val charges = listOf(
            ChargeSent(
                groupId = GROUP_ID,
                memberId = "carla",
                memberName = "Carla",
                amountMinor = 1_000L,
            ),
            ChargeSent(
                groupId = "group-2",
                memberId = "carla-2",
                memberName = "Carla",
                amountMinor = 2_290L,
            ),
        )

        // When
        val result = repository.saveCharges(
            charges = charges,
            actorUserId = USER_ID,
            sentAt = 9_000L,
        )

        // Then
        assertTrue(result.isSuccess)
        val stored = local.events.value
        assertEquals(listOf(GROUP_ID, "group-2"), stored.map { it.groupId })
        assertTrue(stored.all { it.kind == "charge_sent" && it.pendingSync && it.seq == null })
        assertTrue(stored.all { it.actorUserId == USER_ID && it.occurredAt == 9_000L })
        assertNotEquals(stored[0].id, stored[1].id)
    }

    @Test
    fun `GIVEN no account WHEN recording a charge THEN the feed shows it pending with no actor`() = runTest {
        // Given
        prepareScenario()
        val charge = ChargeSent(
            groupId = GROUP_ID,
            memberId = "carla",
            memberName = "Carla",
            amountMinor = 1_000L,
        )

        // When
        repository.saveCharges(
            charges = listOf(charge),
            actorUserId = null,
            sentAt = 9_000L,
        )

        // Then
        val event = repository.observeEvents().first().single()
        assertEquals(ActivityKind.CHARGE_SENT, event.kind)
        assertNull(event.actorUserId)
        assertTrue(event.isPending)
    }

    @Test
    fun `GIVEN the device fails to save WHEN recording a charge THEN returns the failure`() = runTest {
        // Given
        prepareScenario()
        local.writeError = IllegalStateException("database closed")
        val charge = ChargeSent(
            groupId = GROUP_ID,
            memberId = "carla",
            memberName = "Carla",
            amountMinor = 1_000L,
        )

        // When
        val result = repository.saveCharges(
            charges = listOf(charge),
            actorUserId = USER_ID,
            sentAt = 9_000L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN events on the device WHEN loading older ones THEN asks the page of 50 before the oldest`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(
                entity(
                    id = "recent",
                    seq = 120L,
                ),
                entity(
                    id = "oldest-kept",
                    seq = 100L,
                ),
            ),
        )
        remote.olderPages += page(
            from = 99L,
            size = 50,
        )

        // When
        val result = repository.loadOlder()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(100L to 50L), remote.beforeCalls)
        val events = repository.observeEvents().first()
        assertEquals(52, events.size)
        assertEquals(listOf("recent", "oldest-kept", "older-99"), events.take(3).map { it.id })
        assertTrue(repository.observeHasOlder().first())
    }

    @Test
    fun `GIVEN an older page loaded WHEN loading older again THEN asks before the oldest loaded`() = runTest {
        // Given
        prepareScenario(storedEvents = listOf(entity(seq = 100L)))
        remote.olderPages += page(
            from = 99L,
            size = 50,
        )
        remote.olderPages += page(
            from = 49L,
            size = 50,
        )
        repository.loadOlder()

        // When
        repository.loadOlder()

        // Then
        assertEquals(listOf(100L to 50L, 50L to 50L), remote.beforeCalls)
        assertEquals(101, repository.observeEvents().first().size)
    }

    @Test
    fun `GIVEN the server has fewer than a page left WHEN loading older THEN there is nothing older`() = runTest {
        // Given
        prepareScenario(storedEvents = listOf(entity(seq = 100L)))
        remote.olderPages += page(
            from = 99L,
            size = 3,
        )

        // When
        repository.loadOlder()

        // Then
        assertEquals(4, repository.observeEvents().first().size)
        assertFalse(repository.observeHasOlder().first())
    }

    @Test
    fun `GIVEN no event on the device WHEN loading older THEN asks nothing and there is nothing older`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.loadOlder()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.beforeCalls.isEmpty())
        assertFalse(repository.observeHasOlder().first())
    }

    @Test
    fun `GIVEN only a charge not up yet WHEN loading older THEN asks nothing`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(
                entity(
                    id = "charge",
                    seq = null,
                    kind = "charge_sent",
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.loadOlder()

        // Then
        assertTrue(remote.beforeCalls.isEmpty())
        assertFalse(repository.observeHasOlder().first())
    }

    @Test
    fun `GIVEN no connection WHEN loading older THEN fails and the feed still has older to load`() = runTest {
        // Given
        prepareScenario(storedEvents = listOf(entity(seq = 100L)))
        remote.error = IllegalStateException("no connection")

        // When
        val result = repository.loadOlder()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(1, repository.observeEvents().first().size)
        assertTrue(repository.observeHasOlder().first())
    }

    @Test
    fun `GIVEN an older event that a sync also brought to the device WHEN observing THEN shows it once`() = runTest {
        // Given
        prepareScenario(storedEvents = listOf(entity(seq = 100L)))
        remote.olderPages += listOf(
            dto(
                id = "older",
                seq = 99L,
            ),
        )
        repository.loadOlder()

        // When
        local.events.update {
            it + entity(
                id = "older",
                seq = 99L,
            )
        }

        // Then
        assertEquals(listOf(EVENT_ID, "older"), repository.observeEvents().first().map { it.id })
    }

    @Test
    fun `GIVEN an older page with a kind unknown WHEN loading it THEN leaves that event out`() = runTest {
        // Given
        prepareScenario(storedEvents = listOf(entity(seq = 100L)))
        remote.olderPages += listOf(
            dto(
                id = "known",
                seq = 99L,
            ),
            dto(
                id = "unknown",
                seq = 98L,
                kind = "comment_added",
            ),
        )

        // When
        repository.loadOlder()

        // Then
        assertEquals(listOf(EVENT_ID, "known"), repository.observeEvents().first().map { it.id })
    }

    @Test
    fun `GIVEN a charge sent before any account WHEN uploading THEN sends it from the user and it is synced`() =
        runTest {
            // Given
            prepareScenario(
                storedEvents = listOf(
                    entity(
                        id = "charge",
                        seq = null,
                        kind = "charge_sent",
                        actorUserId = null,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val result = repository.uploadPendingActivity(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("charge" to USER_ID), remote.insertedCharges.map { it.id to it.actorUserId })
            assertFalse(
                local.events.value
                    .single()
                    .pendingSync,
            )
        }

    @Test
    fun `GIVEN events opened here WHEN uploading THEN sends the reads of the user and they are synced`() = runTest {
        // Given
        prepareScenario()
        local.reads.value = mapOf("opened-here" to true, "opened-elsewhere" to false)

        // When
        repository.uploadPendingActivity(USER_ID)

        // Then
        assertEquals(
            listOf(
                ActivityReadDto(
                    userId = USER_ID,
                    eventId = "opened-here",
                ),
            ),
            remote.insertedReads,
        )
        assertEquals(mapOf("opened-here" to false, "opened-elsewhere" to false), local.reads.value)
    }

    @Test
    fun `GIVEN all marked read here WHEN uploading THEN sends the mark with its time and it is synced`() = runTest {
        // Given
        prepareScenario(
            storedState = state(
                readThroughSeq = 12L,
                markUpdatedAt = 5_000L,
                markPendingSync = true,
            ),
        )

        // When
        repository.uploadPendingActivity(USER_ID)

        // Then
        assertEquals(
            listOf(
                ActivityReadMarkDto(
                    userId = USER_ID,
                    readThroughSeq = 12L,
                    updatedAt = 5_000L,
                ),
            ),
            remote.upsertedMarks,
        )
        assertEquals(listOf(5_000L), local.markSyncedCalls)
    }

    @Test
    fun `GIVEN nothing pending WHEN uploading THEN sends nothing`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(entity(seq = 5L)),
            storedState = state(readThroughSeq = 5L),
        )
        local.reads.value = mapOf("opened-elsewhere" to false)

        // When
        val result = repository.uploadPendingActivity(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.insertedCharges.isEmpty())
        assertTrue(remote.insertedReads.isEmpty())
        assertTrue(remote.upsertedMarks.isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN uploading a charge THEN fails and the charge stays pending`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(
                entity(
                    id = "charge",
                    seq = null,
                    kind = "charge_sent",
                    pendingSync = true,
                ),
            ),
        )
        remote.error = IllegalStateException("server down")

        // When
        val result = repository.uploadPendingActivity(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(
            local.events.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN the server fails WHEN uploading the reads and the mark THEN both stay pending`() = runTest {
        // Given
        prepareScenario(
            storedState = state(
                readThroughSeq = 12L,
                markUpdatedAt = 5_000L,
                markPendingSync = true,
            ),
        )
        local.reads.value = mapOf("opened-here" to true)
        remote.error = IllegalStateException("server down")

        // When
        val result = repository.uploadPendingActivity(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(mapOf("opened-here" to true), local.reads.value)
        assertTrue(local.markSyncedCalls.isEmpty())
    }

    @Test
    fun `GIVEN a first download WHEN downloading THEN brings the last 90 days and the feed came down for the user`() =
        runTest {
            // Given
            prepareScenario()
            remote.eventsAfter = listOf(
                dto(
                    id = "event-1",
                    seq = 1L,
                ),
                dto(
                    id = "event-2",
                    seq = 2L,
                ),
            )

            // When
            val result = repository.downloadActivity(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(0L to ninetyDaysAgo), remote.afterCalls)
            assertEquals(
                setOf("event-1", "event-2"),
                local.events.value
                    .map { it.id }
                    .toSet(),
            )
            assertTrue(local.events.value.none { it.pendingSync })
            assertEquals(USER_ID, local.state.value?.ownerUserId)
            assertTrue(repository.observeReadState().first().hasDownloaded)
        }

    @Test
    fun `GIVEN the feed already on the device WHEN downloading THEN asks only after the newest event it has`() =
        runTest {
            // Given
            prepareScenario(
                storedEvents = listOf(
                    entity(
                        id = "event-1",
                        seq = 7L,
                    ),
                    entity(
                        id = "event-2",
                        seq = 3L,
                    ),
                ),
                storedState = state(ownerUserId = USER_ID),
            )

            // When
            repository.downloadActivity(USER_ID)

            // Then
            assertEquals(listOf(7L to ninetyDaysAgo), remote.afterCalls)
            assertEquals(0, local.clearCount)
        }

    @Test
    fun `GIVEN the feed of another account on the device WHEN downloading THEN drops it and brings the user one`() =
        runTest {
            // Given
            prepareScenario(
                storedEvents = listOf(
                    entity(
                        id = "theirs",
                        seq = 7L,
                    ),
                ),
                storedState = state(
                    ownerUserId = "other-user",
                    readThroughSeq = 7L,
                ),
            )
            remote.eventsAfter = listOf(
                dto(
                    id = "mine",
                    seq = 3L,
                ),
            )

            // When
            repository.downloadActivity(USER_ID)

            // Then
            assertEquals(1, local.clearCount)
            assertEquals(listOf(0L to ninetyDaysAgo), remote.afterCalls)
            assertEquals(listOf("mine"), local.events.value.map { it.id })
            assertEquals(USER_ID, local.state.value?.ownerUserId)
        }

    @Test
    fun `GIVEN older pages of another account loaded WHEN downloading the user feed THEN forgets them`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(entity(seq = 100L)),
            storedState = state(ownerUserId = "other-user"),
        )
        remote.olderPages += page(
            from = 99L,
            size = 3,
        )
        repository.loadOlder()

        // When
        repository.downloadActivity(USER_ID)

        // Then
        assertTrue(repository.observeEvents().first().isEmpty())
        assertTrue(repository.observeHasOlder().first())
    }

    @Test
    fun `GIVEN a charge sent before the first sync WHEN downloading THEN keeps it`() = runTest {
        // Given
        prepareScenario(
            storedEvents = listOf(
                entity(
                    id = "charge",
                    seq = null,
                    kind = "charge_sent",
                    actorUserId = null,
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.downloadActivity(USER_ID)

        // Then
        assertEquals(0, local.clearCount)
        assertEquals(listOf("charge"), local.events.value.map { it.id })
    }

    @Test
    fun `GIVEN a mark from another device WHEN downloading THEN applies it`() = runTest {
        // Given
        prepareScenario()
        remote.readMark = ActivityReadMarkDto(
            userId = USER_ID,
            readThroughSeq = 30L,
            updatedAt = 4_000L,
        )

        // When
        repository.downloadActivity(USER_ID)

        // Then
        assertEquals(listOf(30L), local.appliedRemoteMarks)
    }

    @Test
    fun `GIVEN no mark on the server WHEN downloading THEN applies none`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.downloadActivity(USER_ID)

        // Then
        assertTrue(local.appliedRemoteMarks.isEmpty())
    }

    @Test
    fun `GIVEN events opened on another device WHEN downloading THEN they are read and synced`() = runTest {
        // Given
        prepareScenario()
        remote.readEventIds = listOf("event-1", "event-2")

        // When
        repository.downloadActivity(USER_ID)

        // Then
        assertEquals(mapOf("event-1" to false, "event-2" to false), local.reads.value)
    }

    @Test
    fun `GIVEN a group joined after the first download WHEN downloading THEN brings its history up to the cursor`() =
        runTest {
            // Given
            prepareScenario(
                storedEvents = listOf(entity(seq = 40L)),
                storedState = state(ownerUserId = USER_ID),
                deviceGroupIds = listOf(GROUP_ID, "joined"),
                backfilledGroupIds = setOf(GROUP_ID),
            )
            remote.groupEvents = listOf(
                dto(
                    id = "joined-history",
                    seq = 12L,
                    groupId = "joined",
                ),
            )

            // When
            repository.downloadActivity(USER_ID)

            // Then
            assertEquals(listOf(Triple(listOf("joined"), 40L, ninetyDaysAgo)), remote.groupCalls)
            assertTrue(local.events.value.any { it.id == "joined-history" })
            assertEquals(setOf(GROUP_ID, "joined"), local.backfilled)
        }

    @Test
    fun `GIVEN a joined group already brought WHEN downloading again THEN does not bring its history again`() =
        runTest {
            // Given
            prepareScenario(
                storedEvents = listOf(entity(seq = 40L)),
                storedState = state(ownerUserId = USER_ID),
                deviceGroupIds = listOf(GROUP_ID, "joined"),
                backfilledGroupIds = setOf(GROUP_ID),
            )
            repository.downloadActivity(USER_ID)

            // When
            repository.downloadActivity(USER_ID)

            // Then
            assertEquals(1, remote.groupCalls.size)
        }

    @Test
    fun `GIVEN the first download WHEN downloading THEN asks no group history and every group counts as brought`() =
        runTest {
            // Given
            prepareScenario(deviceGroupIds = listOf(GROUP_ID, "group-2"))
            remote.eventsAfter = listOf(dto(seq = 5L))

            // When
            repository.downloadActivity(USER_ID)

            // Then
            assertTrue(remote.groupCalls.isEmpty())
            assertEquals(setOf(GROUP_ID, "group-2"), local.backfilled)
        }

    @Test
    fun `GIVEN a download WHEN it ends THEN prunes what is older than 90 days`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.downloadActivity(USER_ID)

        // Then
        assertEquals(listOf(ninetyDaysAgo), local.pruneCutoffs)
    }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN fails and the feed has not come down`() = runTest {
        // Given
        prepareScenario()
        remote.error = IllegalStateException("server down")

        // When
        val result = repository.downloadActivity(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertFalse(repository.observeReadState().first().hasDownloaded)
        assertTrue(local.pruneCutoffs.isEmpty())
    }

    @Test
    fun `GIVEN events and older pages loaded WHEN clearing THEN nothing is left and older can load again`() = runTest {
        // Given
        prepareScenario(storedEvents = listOf(entity(seq = 100L)))
        remote.olderPages += page(
            from = 99L,
            size = 3,
        )
        repository.loadOlder()

        // When
        repository.clear()

        // Then
        assertEquals(1, local.clearCount)
        assertTrue(repository.observeEvents().first().isEmpty())
        assertTrue(repository.observeHasOlder().first())
    }

    private fun page(
        from: Long,
        size: Int,
    ): List<ActivityEventDto> = (0 until size).map { index ->
        val seq = from - index
        dto(
            id = "older-$seq",
            seq = seq,
        )
    }

    private fun dto(
        id: String = EVENT_ID,
        seq: Long,
        kind: String = "bill_created",
        groupId: String = GROUP_ID,
    ): ActivityEventDto = ActivityEventDto(
        id = id,
        seq = seq,
        groupId = groupId,
        kind = kind,
        actorUserId = "ana-user",
        actorMemberId = "ana",
        targetId = "bill-1",
        occurredAt = now - 1_000L,
        data = JsonObject(emptyMap()),
    )

    private fun entity(
        id: String = EVENT_ID,
        seq: Long?,
        kind: String = "bill_created",
        actorUserId: String? = "ana-user",
        pendingSync: Boolean = false,
    ): ActivityEventEntity = ActivityEventEntity(
        id = id,
        seq = seq,
        groupId = GROUP_ID,
        kind = kind,
        actorUserId = actorUserId,
        actorMemberId = "ana",
        targetId = "bill-1",
        occurredAt = now - 1_000L,
        data = "{}",
        pendingSync = pendingSync,
    )

    private fun state(
        ownerUserId: String? = null,
        readThroughSeq: Long = 0L,
        markUpdatedAt: Long = 0L,
        markPendingSync: Boolean = false,
        hasDownloaded: Boolean = false,
    ): ActivityStateEntity = ActivityStateEntity(
        id = "single",
        ownerUserId = ownerUserId,
        readThroughSeq = readThroughSeq,
        markUpdatedAt = markUpdatedAt,
        markPendingSync = markPendingSync,
        hasDownloaded = hasDownloaded,
    )

    private fun billRow(updatedAt: Long): QueuedBillRow = QueuedBillRow(
        id = "bill-1",
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 31_200L,
        splitMode = "equal",
        isDeleted = false,
        updatedAt = updatedAt,
        hasEvent = false,
    )

    private fun paymentRow(updatedAt: Long): QueuedPaymentRow = QueuedPaymentRow(
        id = "payment-1",
        groupId = GROUP_ID,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = "pix",
        isDeleted = false,
        updatedAt = updatedAt,
    )

    private fun groupRow(updatedAt: Long): QueuedGroupRow = QueuedGroupRow(
        id = GROUP_ID,
        name = "Apê 302",
        type = "home",
        isNew = true,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        storedEvents: List<ActivityEventEntity> = emptyList(),
        storedState: ActivityStateEntity? = null,
        deviceGroupIds: List<String> = emptyList(),
        backfilledGroupIds: Set<String> = emptySet(),
    ) {
        local = FakeActivityLocalDataSource().also {
            it.events.value = storedEvents
            it.state.value = storedState
            it.deviceGroupIds = deviceGroupIds
            it.backfilled += backfilledGroupIds
        }
        remote = FakeActivityRemoteDataSource()
        repository = ActivityRepositoryImpl(
            local = local,
            remote = remote,
            mapper = ActivityMapperImpl(),
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val EVENT_ID = "event-1"
        const val GROUP_ID = "group-1"
    }
}

// Why: in memory, with the SQL rules (the mark only moving forward, pruning) left to RoomActivityLocalDataSourceTest.
private class FakeActivityLocalDataSource : ActivityLocalDataSource {
    val events = MutableStateFlow<List<ActivityEventEntity>>(emptyList())

    // Why: event id to whether the read waits to go up.
    val reads = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val state = MutableStateFlow<ActivityStateEntity?>(null)
    val queuedBills = MutableStateFlow<List<QueuedBillRow>>(emptyList())
    val queuedPayments = MutableStateFlow<List<QueuedPaymentRow>>(emptyList())
    val queuedGroups = MutableStateFlow<List<QueuedGroupRow>>(emptyList())
    val liveBillIds = MutableStateFlow<List<String>>(emptyList())
    val liveGroupIds = MutableStateFlow<List<String>>(emptyList())
    var deviceGroupIds: List<String> = emptyList()
    val backfilled = mutableSetOf<String>()
    val markAllCalls = mutableListOf<Pair<Long, Long>>()
    val appliedRemoteMarks = mutableListOf<Long>()
    val markSyncedCalls = mutableListOf<Long>()
    val pruneCutoffs = mutableListOf<Long>()
    var clearCount = 0
    var writeError: Throwable? = null

    override fun observeEvents(): Flow<List<ActivityEventEntity>> = events

    override fun observeReadEventIds(): Flow<List<String>> = reads.map { it.keys.toList() }

    override fun observeState(): Flow<ActivityStateEntity?> = state

    override fun observeQueuedBills(): Flow<List<QueuedBillRow>> = queuedBills

    override fun observeQueuedPayments(): Flow<List<QueuedPaymentRow>> = queuedPayments

    override fun observeQueuedGroups(): Flow<List<QueuedGroupRow>> = queuedGroups

    override fun observeLiveBillIds(): Flow<List<String>> = liveBillIds

    override fun observeLiveGroupIds(): Flow<List<String>> = liveGroupIds

    override suspend fun getState(): ActivityStateEntity? = state.value

    override suspend fun getMaxSeq(): Long? = events.value.mapNotNull { it.seq }.maxOrNull()

    override suspend fun getOldestSeqOrNull(): Long? = events.value.mapNotNull { it.seq }.minOrNull()

    override suspend fun getPendingEvents(): List<ActivityEventEntity> = events.value.filter { it.pendingSync }

    override suspend fun getPendingReadIds(): List<String> = reads.value
        .filterValues { it }
        .keys
        .toList()

    override suspend fun getGroupIdsToBackfill(): List<String> = deviceGroupIds.filterNot { it in backfilled }

    override suspend fun markBackfilled(groupIds: List<String>) {
        backfilled += groupIds
    }

    override suspend fun upsertEvents(entities: List<ActivityEventEntity>) {
        val ids = entities.mapTo(HashSet()) { it.id }
        events.update { stored -> stored.filterNot { it.id in ids } + entities }
    }

    override suspend fun insertEvents(entities: List<ActivityEventEntity>) {
        writeError?.let { throw it }
        events.update { it + entities }
    }

    override suspend fun markRead(eventId: String) {
        writeError?.let { throw it }
        reads.update { it + (eventId to true) }
    }

    override suspend fun insertSyncedReads(eventIds: List<String>) {
        reads.update { stored -> stored + eventIds.filterNot { it in stored }.associateWith { false } }
    }

    override suspend fun markAllRead(
        throughSeq: Long,
        now: Long,
    ) {
        markAllCalls += throughSeq to now
    }

    override suspend fun applyRemoteMark(throughSeq: Long) {
        appliedRemoteMarks += throughSeq
    }

    override suspend fun markEventsSynced(ids: List<String>) {
        events.update { stored -> stored.map { if (it.id in ids) it.copy(pendingSync = false) else it } }
    }

    override suspend fun markReadsSynced(ids: List<String>) {
        reads.update { stored -> stored.mapValues { (id, pending) -> pending && id !in ids } }
    }

    override suspend fun markMarkSynced(syncedUpdatedAt: Long) {
        markSyncedCalls += syncedUpdatedAt
    }

    override suspend fun markDownloaded(userId: String) {
        state.update {
            (it ?: ActivityStateEntity("single", null, 0L, 0L, false, false)).copy(
                ownerUserId = userId,
                hasDownloaded = true,
            )
        }
    }

    override suspend fun prune(cutoff: Long) {
        pruneCutoffs += cutoff
    }

    override suspend fun clear() {
        clearCount++
        events.value = emptyList()
        reads.value = emptyMap()
        state.value = null
        backfilled.clear()
    }
}

private class FakeActivityRemoteDataSource : ActivityRemoteDataSource {
    var eventsAfter: List<ActivityEventDto> = emptyList()
    var groupEvents: List<ActivityEventDto> = emptyList()

    // Why: each loadOlder takes the next page.
    val olderPages = ArrayDeque<List<ActivityEventDto>>()
    var readEventIds: List<String> = emptyList()
    var readMark: ActivityReadMarkDto? = null
    var error: Throwable? = null
    val afterCalls = mutableListOf<Pair<Long, Long>>()
    val groupCalls = mutableListOf<Triple<List<String>, Long, Long>>()
    val beforeCalls = mutableListOf<Pair<Long, Long>>()
    val insertedCharges = mutableListOf<ChargeEventDto>()
    val insertedReads = mutableListOf<ActivityReadDto>()
    val upsertedMarks = mutableListOf<ActivityReadMarkDto>()

    override suspend fun fetchEventsAfter(
        afterSeq: Long,
        since: Long,
    ): List<ActivityEventDto> {
        failIfDown()
        afterCalls += afterSeq to since
        return eventsAfter
    }

    override suspend fun fetchGroupEvents(
        groupIds: List<String>,
        throughSeq: Long,
        since: Long,
    ): List<ActivityEventDto> {
        failIfDown()
        groupCalls += Triple(groupIds, throughSeq, since)
        return groupEvents
    }

    override suspend fun fetchEventsBefore(
        beforeSeq: Long,
        limit: Long,
    ): List<ActivityEventDto> {
        failIfDown()
        beforeCalls += beforeSeq to limit
        return olderPages.removeFirstOrNull().orEmpty()
    }

    override suspend fun insertCharges(events: List<ChargeEventDto>) {
        failIfDown()
        insertedCharges += events
    }

    override suspend fun fetchReadEventIds(): List<String> {
        failIfDown()
        return readEventIds
    }

    override suspend fun insertReads(reads: List<ActivityReadDto>) {
        failIfDown()
        insertedReads += reads
    }

    override suspend fun fetchReadMark(): ActivityReadMarkDto? {
        failIfDown()
        return readMark
    }

    override suspend fun upsertReadMark(mark: ActivityReadMarkDto) {
        failIfDown()
        upsertedMarks += mark
    }

    private fun failIfDown() {
        error?.let { throw it }
    }
}
