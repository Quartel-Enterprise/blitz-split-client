package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.BillAttachmentLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillAttachmentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.data.mapper.BillAttachmentMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.BillAttachmentRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.PendingJpeg
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class BillAttachmentRepositoryImplTest {
    private lateinit var local: FakeBillAttachmentLocalDataSource
    private lateinit var remote: FakeBillAttachmentRemoteDataSource
    private lateinit var repository: BillAttachmentRepositoryImpl
    private var now = START

    @Test
    fun `GIVEN a photo removed while it first goes up WHEN uploading THEN takes it off the server too`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "photo-1",
                    updatedAt = 5L,
                    pendingJpeg = jpeg,
                    pendingSync = true,
                ),
            ),
        )
        // Why: as BillAttachmentDao.remove does for a photo still pending with its JPEG.
        remote.onUpsert = { local.entities.update { list -> list.filterNot { it.id == "photo-1" } } }

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("upload:$PATH_1", "upsert:photo-1", "upsert:photo-1", "remove:$PATH_1"), remote.calls)
        val tombstone = remote.upserted.last()
        assertTrue(tombstone.isDeleted)
        assertEquals(6L, tombstone.updatedAt)
    }

    @Test
    fun `GIVEN attachments of a kind this version does not know WHEN observing the bill THEN leaves them out`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "receipt-1",
                        kind = "receipt",
                    ),
                    entity(
                        id = "video-1",
                        kind = "video",
                    ),
                    entity(id = "photo-1"),
                ),
            )

            // When
            val attachments = repository.observeAttachments(BILL_ID).first()

            // Then
            assertEquals(listOf("receipt-1", "photo-1"), attachments.map { it.id })
            assertEquals(
                listOf(BillAttachmentKind.RECEIPT, BillAttachmentKind.PHOTO),
                attachments.map { it.kind },
            )
        }

    @Test
    fun `GIVEN a photo added offline WHEN observing the bill THEN shows it pending with its image`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "photo-1",
                    pendingJpeg = jpeg,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val attachment = repository.observeAttachments(BILL_ID).first().single()

        // Then
        assertTrue(attachment.isPending)
        assertEquals(PendingJpeg(jpeg), attachment.pendingImage)
    }

    @Test
    fun `GIVEN changes waiting to go up WHEN observing the pending versions THEN lists each id with its time`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "photo-1",
                        updatedAt = 5L,
                        pendingSync = true,
                    ),
                    entity(id = "photo-2"),
                    entity(
                        id = "photo-3",
                        updatedAt = 7L,
                        isDeleted = true,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val versions = repository.observePendingVersions().first()

            // Then
            assertEquals(listOf("photo-1:5", "photo-3:7"), versions)
        }

    @Test
    fun `GIVEN a removal waiting to go up WHEN observing pending changes THEN there are some`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "photo-1",
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPending)
    }

    @Test
    fun `GIVEN only synced attachments WHEN observing pending changes THEN there are none`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "photo-1")))

        // When
        val hasPending = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPending)
    }

    @Test
    fun `GIVEN no internet WHEN adding a photo THEN the device stores it pending within 5 photos`() = runTest {
        // Given
        prepareScenario()
        val photo = attachment(
            id = "photo-1",
            kind = BillAttachmentKind.PHOTO,
            pendingImage = PendingJpeg(jpeg),
        )

        // When
        val result = repository.addAttachment(photo)

        // Then
        assertEquals(true, result.getOrNull())
        assertEquals(listOf("photo-1" to 5), local.insertLimits)
        val stored = local.entities.value.single()
        assertTrue(stored.pendingSync)
        assertTrue(jpeg.contentEquals(stored.pendingJpeg))
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun `GIVEN a receipt WHEN adding it THEN the bill takes a single one`() = runTest {
        // Given
        prepareScenario()
        val receipt = attachment(
            id = "receipt-1",
            kind = BillAttachmentKind.RECEIPT,
            pendingImage = PendingJpeg(jpeg),
        )

        // When
        repository.addAttachment(receipt)

        // Then
        assertEquals(listOf("receipt-1" to 1), local.insertLimits)
    }

    @Test
    fun `GIVEN a bill with its 5 photos WHEN adding another THEN answers there is no room and stores nothing`() =
        runTest {
            // Given
            prepareScenario(stored = (1..5).map { entity(id = "photo-$it") })
            val photo = attachment(
                id = "photo-6",
                kind = BillAttachmentKind.PHOTO,
            )

            // When
            val result = repository.addAttachment(photo)

            // Then
            assertEquals(false, result.getOrNull())
            assertEquals(5, local.entities.value.size)
        }

    @Test
    fun `GIVEN the bill is gone from the device WHEN adding a photo THEN fails`() = runTest {
        // Given
        prepareScenario(insertError = IllegalStateException("No bill bill-1"))
        val photo = attachment(
            id = "photo-1",
            kind = BillAttachmentKind.PHOTO,
            pendingImage = PendingJpeg(jpeg),
        )

        // When
        val result = repository.addAttachment(photo)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.entities.value.isEmpty())
    }

    @Test
    fun `GIVEN no internet WHEN removing a photo THEN the device takes it off at that time`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "photo-1")))

        // When
        val result = repository.removeAttachment("photo-1", updatedAt = 9_000L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("photo-1" to 9_000L), local.removals)
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun `GIVEN the device fails WHEN removing a photo THEN fails`() = runTest {
        // Given
        prepareScenario(removeError = IllegalStateException("disk"))

        // When
        val result = repository.removeAttachment("photo-1", updatedAt = 9_000L)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a photo added offline WHEN uploading THEN sends the image then its row and marks that version synced`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "photo-1",
                        addedByUserId = null,
                        updatedAt = 5L,
                        pendingJpeg = jpeg,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("upload:$PATH_1", "upsert:photo-1"), remote.calls)
            assertTrue(jpeg.contentEquals(remote.uploadedImages.getValue(PATH_1)))
            val row = remote.upserted.single()
            assertEquals(USER_ID to false, row.addedByUserId to row.isDeleted)
            assertEquals(listOf("photo-1" to 5L), local.markedSynced)
        }

    @Test
    fun `GIVEN a change with no image waiting WHEN uploading THEN sends only its row`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "photo-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("upsert:photo-1"), remote.calls)
    }

    @Test
    fun `GIVEN a photo removed offline WHEN uploading THEN sends the removal before deleting its image`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                entity(
                    id = "photo-1",
                    addedByUserId = "user-ana",
                    updatedAt = 8L,
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("upsert:photo-1", "remove:$PATH_1"), remote.calls)
        val row = remote.upserted.single()
        assertEquals(USER_ID to true, row.addedByUserId to row.isDeleted)
        assertEquals(listOf("photo-1" to 8L), local.markedSynced)
    }

    @Test
    fun `GIVEN the bucket fails to delete a removed image WHEN uploading THEN the removal still counts as synced`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    entity(
                        id = "photo-1",
                        updatedAt = 8L,
                        isDeleted = true,
                        pendingSync = true,
                    ),
                ),
                removeImageError = IllegalStateException("bucket"),
            )

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("photo-1" to 8L), local.markedSynced)
        }

    @Test
    fun `GIVEN the bucket refuses one image WHEN uploading THEN the others go up and the sync fails after them`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    pendingPhoto(id = "photo-1"),
                    pendingPhoto(id = "photo-2"),
                    pendingPhoto(id = "photo-3"),
                ),
                uploadErrors = mapOf(PATH_2 to IllegalStateException("too large")),
            )

            // When
            val result = repository.uploadPending(USER_ID)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(listOf("photo-1", "photo-3"), remote.upserted.map { it.id })
            assertEquals(listOf("photo-1", "photo-3"), local.markedSynced.map { it.first })
            assertEquals(
                listOf("photo-1" to false, "photo-2" to true, "photo-3" to false),
                local.entities.value.map { it.id to it.pendingSync },
            )
        }

    @Test
    fun `GIVEN the server refuses one row WHEN uploading THEN the others go up and that one stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pendingPhoto(id = "photo-1"),
                pendingPhoto(id = "photo-2"),
            ),
            upsertErrors = mapOf("photo-1" to IllegalArgumentException("policy")),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertEquals(listOf("photo-2"), local.markedSynced.map { it.first })
        assertEquals(
            listOf("photo-1" to true, "photo-2" to false),
            local.entities.value.map { it.id to it.pendingSync },
        )
    }

    @Test
    fun `GIVEN two attachments failing WHEN uploading THEN fails with the first error`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pendingPhoto(id = "photo-1"),
                pendingPhoto(id = "photo-2"),
            ),
            uploadErrors = mapOf(PATH_1 to IllegalStateException("first")),
            upsertErrors = mapOf("photo-2" to IllegalArgumentException("second")),
        )

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        val error = result.exceptionOrNull()
        assertIs<IllegalStateException>(error)
        assertEquals("first", error.message)
        assertTrue(local.markedSynced.isEmpty())
    }

    @Test
    fun `GIVEN pending attachments of two accounts groups WHEN uploading THEN sends only the callers`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                pendingPhoto(id = "mine"),
                pendingPhoto(
                    id = "theirs",
                    groupId = OTHER_GROUP_ID,
                ),
            ),
        )

        // When
        repository.uploadPending(USER_ID)

        // Then
        assertEquals(listOf(USER_ID), local.pendingOwners)
        assertEquals(listOf("mine"), remote.upserted.map { it.id })
        assertEquals(listOf("mine"), local.markedSynced.map { it.first })
    }

    @Test
    fun `GIVEN nothing waiting to go up WHEN uploading THEN calls nothing on the server`() = runTest {
        // Given
        prepareScenario(stored = listOf(entity(id = "photo-1")))

        // When
        val result = repository.uploadPending(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.calls.isEmpty())
        assertTrue(local.markedSynced.isEmpty())
    }

    @Test
    fun `GIVEN attachments and removals on the server WHEN downloading THEN applies every row synced`() = runTest {
        // Given
        prepareScenario(
            serverRows = listOf(
                dto(id = "s-1"),
                dto(
                    id = "s-2",
                    isDeleted = true,
                ),
            ),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertTrue(result.isSuccess)
        val applied = local.applied.single()
        assertEquals(listOf("s-1" to false, "s-2" to true), applied.map { it.id to it.isDeleted })
        assertTrue(applied.none { it.pendingSync || it.pendingJpeg != null })
        assertEquals(listOf(USER_ID, USER_ID), applied.map { it.addedByUserId })
    }

    @Test
    fun `GIVEN attachments and removals on the server WHEN downloading THEN keeps only the live ones as still there`() =
        runTest {
            // Given
            prepareScenario(
                serverRows = listOf(
                    dto(id = "s-1"),
                    dto(
                        id = "s-2",
                        isDeleted = true,
                    ),
                    dto(id = "s-3"),
                ),
            )

            // When
            repository.download(USER_ID)

            // Then
            assertEquals(listOf(USER_ID to setOf("s-1", "s-3")), local.goneCalls)
        }

    @Test
    fun `GIVEN the server has no attachment WHEN downloading THEN drops every synced one of the users groups`() =
        runTest {
            // Given
            prepareScenario(stored = listOf(entity(id = "photo-1")))

            // When
            val result = repository.download(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(USER_ID to emptySet<String>()), local.goneCalls)
        }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN fails and changes nothing on the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(entity(id = "photo-1")),
            fetchError = IllegalStateException("network"),
        )

        // When
        val result = repository.download(USER_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.applied.isEmpty())
        assertTrue(local.goneCalls.isEmpty())
    }

    @Test
    fun `GIVEN photos up and one not up yet WHEN loading the URLs THEN signs only those up for 2 hours`() = runTest {
        // Given
        prepareScenario()
        val attachments = listOf(
            attachment(id = "photo-1"),
            attachment(
                id = "photo-2",
                pendingImage = PendingJpeg(jpeg),
            ),
            attachment(id = "photo-3"),
        )

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertEquals(
            mapOf("photo-1" to signed(PATH_1, 1), "photo-3" to signed(PATH_3, 1)),
            result.getOrNull(),
        )
        assertEquals(listOf(listOf(PATH_1, PATH_3) to 2.hours), remote.signRequests)
    }

    @Test
    fun `GIVEN only photos not up yet WHEN loading the URLs THEN asks the server nothing`() = runTest {
        // Given
        prepareScenario()
        val attachments = listOf(
            attachment(
                id = "photo-1",
                pendingImage = PendingJpeg(jpeg),
            ),
        )

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertEquals(emptyMap(), result.getOrNull())
        assertTrue(remote.signRequests.isEmpty())
    }

    @Test
    fun `GIVEN a URL with more than 30 minutes left WHEN loading it again THEN reuses it`() = runTest {
        // Given
        prepareScenario()
        val attachments = listOf(attachment(id = "photo-1"))
        repository.getImageUrls(attachments)
        now = START + 90.minutes.inWholeMilliseconds - 1

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertEquals(mapOf("photo-1" to signed(PATH_1, 1)), result.getOrNull())
        assertEquals(1, remote.signRequests.size)
    }

    @Test
    fun `GIVEN a URL with 30 minutes left WHEN loading it again THEN signs it again`() = runTest {
        // Given
        prepareScenario()
        val attachments = listOf(attachment(id = "photo-1"))
        repository.getImageUrls(attachments)
        now = START + 90.minutes.inWholeMilliseconds

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertEquals(mapOf("photo-1" to signed(PATH_1, 2)), result.getOrNull())
        assertEquals(listOf(PATH_1), remote.signRequests.last().first)
    }

    @Test
    fun `GIVEN one URL already signed WHEN loading it with a new photo THEN signs only the new one`() = runTest {
        // Given
        prepareScenario()
        repository.getImageUrls(listOf(attachment(id = "photo-1")))
        val attachments = listOf(attachment(id = "photo-1"), attachment(id = "photo-2"))

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertEquals(
            mapOf("photo-1" to signed(PATH_1, 1), "photo-2" to signed(PATH_2, 2)),
            result.getOrNull(),
        )
        assertEquals(listOf(listOf(PATH_1), listOf(PATH_2)), remote.signRequests.map { it.first })
    }

    @Test
    fun `GIVEN an image the bucket cannot sign WHEN loading the URLs THEN leaves it out`() = runTest {
        // Given
        prepareScenario(unsignablePaths = setOf(PATH_2))
        val attachments = listOf(attachment(id = "photo-1"), attachment(id = "photo-2"))

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertEquals(mapOf("photo-1" to signed(PATH_1, 1)), result.getOrNull())
    }

    @Test
    fun `GIVEN an image the bucket could not sign before WHEN loading the URLs again THEN asks for it again`() =
        runTest {
            // Given
            prepareScenario(unsignablePaths = setOf(PATH_2))
            val attachments = listOf(attachment(id = "photo-1"), attachment(id = "photo-2"))
            repository.getImageUrls(attachments)

            // When
            repository.getImageUrls(attachments)

            // Then
            assertEquals(listOf(PATH_2), remote.signRequests.last().first)
        }

    @Test
    fun `GIVEN no internet WHEN loading the URLs THEN fails`() = runTest {
        // Given
        prepareScenario(signError = IllegalStateException("offline"))
        val attachments = listOf(attachment(id = "photo-1"))

        // When
        val result = repository.getImageUrls(attachments)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private fun signed(
        path: String,
        request: Int,
    ): String = "https://signed/$path?request=$request"

    private fun attachment(
        id: String,
        kind: BillAttachmentKind = BillAttachmentKind.PHOTO,
        pendingImage: PendingJpeg? = null,
    ): BillAttachment = BillAttachment(
        id = id,
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        kind = kind,
        width = 800,
        height = 600,
        addedByUserId = null,
        addedByMemberId = "member-ana",
        addedAt = 1L,
        updatedAt = 1L,
        isPending = pendingImage != null,
        pendingImage = pendingImage,
    )

    private fun pendingPhoto(
        id: String,
        groupId: String = GROUP_ID,
    ): BillAttachmentEntity = entity(
        id = id,
        groupId = groupId,
        pendingJpeg = jpeg,
        pendingSync = true,
    )

    private fun entity(
        id: String,
        groupId: String = GROUP_ID,
        kind: String = "photo",
        addedByUserId: String? = USER_ID,
        updatedAt: Long = 1L,
        isDeleted: Boolean = false,
        pendingJpeg: ByteArray? = null,
        pendingSync: Boolean = false,
    ): BillAttachmentEntity = BillAttachmentEntity(
        id = id,
        expenseId = BILL_ID,
        groupId = groupId,
        kind = kind,
        width = 800,
        height = 600,
        addedByUserId = addedByUserId,
        addedByMemberId = "member-ana",
        addedAt = 1L,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        pendingJpeg = pendingJpeg,
        pendingSync = pendingSync,
    )

    private fun dto(
        id: String,
        isDeleted: Boolean = false,
    ): BillAttachmentDto = BillAttachmentDto(
        id = id,
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        kind = "photo",
        addedByUserId = USER_ID,
        addedByMemberId = "member-ana",
        width = 800,
        height = 600,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = 2L,
    )

    private fun prepareScenario(
        stored: List<BillAttachmentEntity> = emptyList(),
        serverRows: List<BillAttachmentDto> = emptyList(),
        insertError: Throwable? = null,
        removeError: Throwable? = null,
        uploadErrors: Map<String, Throwable> = emptyMap(),
        upsertErrors: Map<String, Throwable> = emptyMap(),
        removeImageError: Throwable? = null,
        fetchError: Throwable? = null,
        unsignablePaths: Set<String> = emptySet(),
        signError: Throwable? = null,
    ) {
        local = FakeBillAttachmentLocalDataSource(
            groupOwners = mapOf(
                GROUP_ID to USER_ID,
                OTHER_GROUP_ID to "user-other",
            ),
            stored = stored,
            insertError = insertError,
            removeError = removeError,
        )
        remote = FakeBillAttachmentRemoteDataSource(
            rows = serverRows,
            uploadErrors = uploadErrors,
            upsertErrors = upsertErrors,
            removeImageError = removeImageError,
            fetchError = fetchError,
            unsignablePaths = unsignablePaths,
            signError = signError,
        )
        repository = BillAttachmentRepositoryImpl(
            local = local,
            remote = remote,
            mapper = BillAttachmentMapperImpl(),
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val START = 1_000_000L
        const val USER_ID = "user-1"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
        const val BILL_ID = "bill-1"
        const val PATH_1 = "group-1/bill-1/photo-1.jpg"
        const val PATH_2 = "group-1/bill-1/photo-2.jpg"
        const val PATH_3 = "group-1/bill-1/photo-3.jpg"
    }
}

// Why: the order, the owner filter and Last-Write-Wins are Room's; this fake only keeps what the repository relies on.
private class FakeBillAttachmentLocalDataSource(
    private val groupOwners: Map<String, String>,
    private val insertError: Throwable?,
    private val removeError: Throwable?,
    stored: List<BillAttachmentEntity>,
) : BillAttachmentLocalDataSource {
    val entities = MutableStateFlow(stored)
    val insertLimits = mutableListOf<Pair<String, Int>>()
    val removals = mutableListOf<Pair<String, Long>>()
    val pendingOwners = mutableListOf<String>()
    val markedSynced = mutableListOf<Pair<String, Long>>()
    val applied = mutableListOf<List<BillAttachmentEntity>>()
    val goneCalls = mutableListOf<Pair<String, Set<String>>>()

    override fun observeAttachments(expenseId: String): Flow<List<BillAttachmentEntity>> = entities.map { list ->
        list.filter { it.expenseId == expenseId && !it.isDeleted }
    }

    override fun observeHasPending(): Flow<Boolean> = entities.map { list -> list.any { it.pendingSync } }

    override fun observePendingVersions(): Flow<List<String>> = entities.map { list ->
        list.filter { it.pendingSync }.map { "${it.id}:${it.updatedAt}" }
    }

    override suspend fun getPendingAttachments(ownerUserId: String): List<BillAttachmentEntity> {
        pendingOwners += ownerUserId
        return entities.value.filter { it.pendingSync && groupOwners[it.groupId] == ownerUserId }
    }

    override suspend fun insertWithinLimit(
        entity: BillAttachmentEntity,
        limit: Int,
    ): Boolean {
        insertLimits += entity.id to limit
        insertError?.let { throw it }
        val live = entities.value.count {
            it.expenseId == entity.expenseId && it.kind == entity.kind && !it.isDeleted
        }
        if (live >= limit) return false
        entities.update { it + entity }
        return true
    }

    override suspend fun remove(
        id: String,
        updatedAt: Long,
    ) {
        removeError?.let { throw it }
        removals += id to updatedAt
    }

    override suspend fun markSynced(
        id: String,
        updatedAt: Long,
    ) {
        markedSynced += id to updatedAt
        entities.update { list ->
            list.map { if (it.id == id && it.updatedAt == updatedAt) it.copy(pendingSync = false) else it }
        }
    }

    override suspend fun isOnDevice(id: String): Boolean = entities.value.any { it.id == id }

    override suspend fun applyRemote(entities: List<BillAttachmentEntity>) {
        applied += entities
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverAttachmentIds: Set<String>,
    ) {
        goneCalls += ownerUserId to serverAttachmentIds
    }
}

private class FakeBillAttachmentRemoteDataSource(
    private val rows: List<BillAttachmentDto>,
    private val uploadErrors: Map<String, Throwable>,
    private val upsertErrors: Map<String, Throwable>,
    private val removeImageError: Throwable?,
    private val fetchError: Throwable?,
    private val unsignablePaths: Set<String>,
    private val signError: Throwable?,
) : BillAttachmentRemoteDataSource {
    // Why: every call to the server in order, so a test checks the image goes before or after its row.
    val calls = mutableListOf<String>()
    val uploadedImages = mutableMapOf<String, ByteArray>()
    val upserted = mutableListOf<BillAttachmentDto>()
    val signRequests = mutableListOf<Pair<List<String>, Duration>>()
    var onUpsert: () -> Unit = {}

    override suspend fun uploadImage(
        path: String,
        jpeg: ByteArray,
    ) {
        calls += "upload:$path"
        uploadErrors[path]?.let { throw it }
        uploadedImages[path] = jpeg
    }

    override suspend fun removeImage(path: String) {
        calls += "remove:$path"
        removeImageError?.let { throw it }
    }

    override suspend fun upsertAttachment(attachment: BillAttachmentDto) {
        calls += "upsert:${attachment.id}"
        upsertErrors[attachment.id]?.let { throw it }
        upserted += attachment
        onUpsert()
        onUpsert = {}
    }

    override suspend fun fetchAttachments(): List<BillAttachmentDto> {
        fetchError?.let { throw it }
        return rows
    }

    override suspend fun createSignedUrls(
        paths: List<String>,
        expiresIn: Duration,
    ): Map<String, String> {
        signError?.let { throw it }
        signRequests += paths to expiresIn
        val request = signRequests.size
        return paths.filterNot { it in unsignablePaths }.associateWith { "https://signed/$it?request=$request" }
    }
}
