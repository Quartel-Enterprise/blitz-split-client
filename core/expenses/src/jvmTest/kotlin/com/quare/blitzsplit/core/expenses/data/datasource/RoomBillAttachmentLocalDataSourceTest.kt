package com.quare.blitzsplit.core.expenses.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the data source over a real in-memory Room, since its rules (the limit, the tombstone, LWW) are SQL.
class RoomBillAttachmentLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomBillAttachmentLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN photos a receipt and a removed one WHEN observing the bill THEN the receipt then by addition`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    attachment(
                        id = "photo-b",
                        addedAt = 2L,
                    ),
                    attachment(
                        id = "photo-late",
                        addedAt = 5L,
                    ),
                    attachment(
                        id = "receipt",
                        kind = RECEIPT,
                        addedAt = 9L,
                    ),
                    attachment(
                        id = "photo-a",
                        addedAt = 2L,
                    ),
                    attachment(
                        id = "removed",
                        addedAt = 1L,
                        isDeleted = true,
                        pendingSync = true,
                    ),
                    attachment(
                        id = "other-bill",
                        expenseId = OTHER_BILL,
                    ),
                ),
            )

            // When
            val attachments = dataSource.observeAttachments(MY_BILL).first()

            // Then
            assertEquals(listOf("receipt", "photo-a", "photo-b", "photo-late"), attachments.map { it.id })
        }

    @Test
    fun `GIVEN a bill on the device WHEN adding a photo THEN stores it pending with its JPEG`() = runTest {
        // Given
        prepareScenario()
        val photo = attachment(
            id = "photo-1",
            pendingJpeg = jpeg,
            pendingSync = true,
        )

        // When
        val added = dataSource.insertWithinLimit(photo, limit = 5)

        // Then
        assertTrue(added)
        val stored = dataSource.observeAttachments(MY_BILL).first().single()
        assertTrue(stored.pendingSync)
        assertTrue(jpeg.contentEquals(stored.pendingJpeg))
        assertTrue(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a bill with its 5 photos WHEN adding another THEN refuses it`() = runTest {
        // Given
        prepareScenario(stored = (1..5).map { attachment(id = "photo-$it") })
        val photo = attachment(
            id = "photo-6",
            pendingJpeg = jpeg,
            pendingSync = true,
        )

        // When
        val added = dataSource.insertWithinLimit(photo, limit = 5)

        // Then
        assertFalse(added)
        assertEquals(5, dataSource.observeAttachments(MY_BILL).first().size)
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN 4 photos and a removed one WHEN adding a photo THEN the removed one leaves room for it`() = runTest {
        // Given
        prepareScenario(
            stored = (1..4).map { attachment(id = "photo-$it") } +
                attachment(
                    id = "removed",
                    isDeleted = true,
                    pendingSync = true,
                ),
        )
        val photo = attachment(
            id = "photo-5",
            pendingSync = true,
        )

        // When
        val added = dataSource.insertWithinLimit(photo, limit = 5)

        // Then
        assertTrue(added)
        assertEquals(5, dataSource.observeAttachments(MY_BILL).first().size)
    }

    @Test
    fun `GIVEN 4 photos and the receipt WHEN adding a photo THEN the receipt is not one of the 5`() = runTest {
        // Given
        prepareScenario(
            stored = (1..4).map { attachment(id = "photo-$it") } +
                attachment(
                    id = "receipt",
                    kind = RECEIPT,
                ),
        )
        val photo = attachment(
            id = "photo-5",
            pendingSync = true,
        )

        // When
        val added = dataSource.insertWithinLimit(photo, limit = 5)

        // Then
        assertTrue(added)
    }

    @Test
    fun `GIVEN 5 photos on another bill WHEN adding a photo to this one THEN stores it`() = runTest {
        // Given
        prepareScenario(
            stored = (1..5).map {
                attachment(
                    id = "photo-$it",
                    expenseId = OTHER_BILL,
                )
            },
        )
        val photo = attachment(
            id = "photo-6",
            pendingSync = true,
        )

        // When
        val added = dataSource.insertWithinLimit(photo, limit = 5)

        // Then
        assertTrue(added)
    }

    @Test
    fun `GIVEN a bill with its receipt WHEN adding another receipt THEN refuses it`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "receipt-1",
                    kind = RECEIPT,
                ),
            ),
        )
        val receipt = attachment(
            id = "receipt-2",
            kind = RECEIPT,
            pendingSync = true,
        )

        // When
        val added = dataSource.insertWithinLimit(receipt, limit = 1)

        // Then
        assertFalse(added)
        assertEquals(listOf("receipt-1"), dataSource.observeAttachments(MY_BILL).first().map { it.id })
    }

    @Test
    fun `GIVEN the bill is gone from the device WHEN adding a photo THEN fails and stores nothing`() = runTest {
        // Given
        prepareScenario()
        val photo = attachment(
            id = "photo-1",
            expenseId = "gone",
            pendingSync = true,
        )

        // When
        val result = runCatching { dataSource.insertWithinLimit(photo, limit = 5) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a photo added offline and not up yet WHEN removing it THEN leaves a pending tombstone`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 5L,
                    pendingJpeg = jpeg,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.remove("photo-1", updatedAt = 9_000L)

        // Then
        // Why: its upload may have reached the server with the answer lost, so the removal must go up too.
        assertTrue(dataSource.observeAttachments(MY_BILL).first().isEmpty())
        val tombstone = getPending().single()
        assertEquals("photo-1" to 9_000L, tombstone.id to tombstone.updatedAt)
        assertTrue(tombstone.isDeleted)
        assertNull(tombstone.pendingJpeg)
    }

    @Test
    fun `GIVEN a synced photo WHEN removing it THEN it leaves the bill as a pending tombstone at that time`() =
        runTest {
            // Given
            prepareScenario(stored = listOf(attachment(id = "photo-1")))

            // When
            dataSource.remove("photo-1", updatedAt = 9_000L)

            // Then
            assertTrue(dataSource.observeAttachments(MY_BILL).first().isEmpty())
            val tombstone = getPending().single()
            assertEquals("photo-1", tombstone.id)
            assertTrue(tombstone.isDeleted)
            assertEquals(9_000L, tombstone.updatedAt)
            assertNull(tombstone.pendingJpeg)
        }

    @Test
    fun `GIVEN a photo stamped later than the clock WHEN removing it THEN the tombstone is still newer`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 10_000L,
                ),
            ),
        )

        // When
        dataSource.remove("photo-1", updatedAt = 9_000L)

        // Then
        assertEquals(
            10_001L,
            getPending().single().updatedAt,
        )
    }

    @Test
    fun `GIVEN no such photo WHEN removing it THEN nothing waits to go up`() = runTest {
        // Given
        prepareScenario()

        // When
        dataSource.remove("photo-1", updatedAt = 9_000L)

        // Then
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a photo that went up WHEN marking that version synced THEN is synced without its JPEG`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 5L,
                    pendingJpeg = jpeg,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.markSynced("photo-1", updatedAt = 5L)

        // Then
        val stored = dataSource.observeAttachments(MY_BILL).first().single()
        assertFalse(stored.pendingSync)
        assertNull(stored.pendingJpeg)
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a photo removed while going up WHEN marking the older version synced THEN removal stays`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 8L,
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.markSynced("photo-1", updatedAt = 5L)

        // Then
        val stored = getPending().single()
        assertEquals("photo-1" to 8L, stored.id to stored.updatedAt)
        assertTrue(stored.isDeleted)
    }

    @Test
    fun `GIVEN a photo changed while going up WHEN marking the older version synced THEN stays pending`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 8L,
                    pendingJpeg = jpeg,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.markSynced("photo-1", updatedAt = 5L)

        // Then
        val stored = dataSource.observeAttachments(MY_BILL).first().single()
        assertTrue(stored.pendingSync)
        assertTrue(jpeg.contentEquals(stored.pendingJpeg))
    }

    @Test
    fun `GIVEN a removal that went up WHEN marking it synced THEN the tombstone leaves the device`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 8L,
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )

        // When
        dataSource.markSynced("photo-1", updatedAt = 8L)

        // Then
        assertNull(database.billAttachmentDao().getAttachment("photo-1"))
        assertFalse(dataSource.observeHasPending().first())
    }

    @Test
    fun `GIVEN a server photo of a bill on the device WHEN applying it THEN the bill shows it synced`() = runTest {
        // Given
        prepareScenario()
        val serverPhoto = attachment(id = "photo-1")

        // When
        dataSource.applyRemote(listOf(serverPhoto))

        // Then
        val stored = dataSource.observeAttachments(MY_BILL).first().single()
        assertEquals("photo-1" to false, stored.id to stored.pendingSync)
    }

    @Test
    fun `GIVEN a server photo of a bill not on the device WHEN applying it THEN skips it`() = runTest {
        // Given
        prepareScenario()
        val serverPhoto = attachment(
            id = "photo-1",
            expenseId = "not-here",
        )

        // When
        dataSource.applyRemote(listOf(serverPhoto))

        // Then
        assertNull(database.billAttachmentDao().getAttachment("photo-1"))
    }

    @Test
    fun `GIVEN a removal waiting to go up WHEN an older server row comes down THEN the removal stays`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    updatedAt = 8L,
                    isDeleted = true,
                    pendingSync = true,
                ),
            ),
        )
        val serverPhoto = attachment(
            id = "photo-1",
            updatedAt = 5L,
        )

        // When
        dataSource.applyRemote(listOf(serverPhoto))

        // Then
        val stored = getPending().single()
        assertEquals(8L to true, stored.updatedAt to stored.isDeleted)
    }

    @Test
    fun `GIVEN a photo waiting to go up WHEN a server row as old comes down THEN the local photo stays`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    width = 100,
                    updatedAt = 5L,
                    pendingJpeg = jpeg,
                    pendingSync = true,
                ),
            ),
        )
        val serverPhoto = attachment(
            id = "photo-1",
            width = 200,
            updatedAt = 5L,
        )

        // When
        dataSource.applyRemote(listOf(serverPhoto))

        // Then
        val stored = dataSource.observeAttachments(MY_BILL).first().single()
        assertEquals(100 to true, stored.width to stored.pendingSync)
        assertTrue(jpeg.contentEquals(stored.pendingJpeg))
    }

    @Test
    fun `GIVEN a photo waiting to go up WHEN a newer server row comes down THEN the server wins`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    width = 100,
                    updatedAt = 5L,
                    pendingSync = true,
                ),
            ),
        )
        val serverPhoto = attachment(
            id = "photo-1",
            width = 200,
            updatedAt = 6L,
        )

        // When
        dataSource.applyRemote(listOf(serverPhoto))

        // Then
        val stored = dataSource.observeAttachments(MY_BILL).first().single()
        assertEquals(200 to false, stored.width to stored.pendingSync)
    }

    @Test
    fun `GIVEN a photo waiting to go up WHEN an older server removal comes down THEN the photo leaves the device`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    attachment(
                        id = "photo-1",
                        updatedAt = 8L,
                        pendingSync = true,
                    ),
                ),
            )
            val serverRemoval = attachment(
                id = "photo-1",
                updatedAt = 5L,
                isDeleted = true,
            )

            // When
            dataSource.applyRemote(listOf(serverRemoval))

            // Then
            assertNull(database.billAttachmentDao().getAttachment("photo-1"))
            assertFalse(dataSource.observeHasPending().first())
        }

    @Test
    fun `GIVEN a synced photo WHEN its server removal comes down THEN it leaves the device`() = runTest {
        // Given
        prepareScenario(stored = listOf(attachment(id = "photo-1")))
        val serverRemoval = attachment(
            id = "photo-1",
            updatedAt = 5L,
            isDeleted = true,
        )

        // When
        dataSource.applyRemote(listOf(serverRemoval))

        // Then
        assertNull(database.billAttachmentDao().getAttachment("photo-1"))
    }

    @Test
    fun `GIVEN a synced photo WHEN the server row comes down changed THEN takes the server version`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                attachment(
                    id = "photo-1",
                    addedByUserId = null,
                    updatedAt = 5L,
                ),
            ),
        )
        val serverPhoto = attachment(
            id = "photo-1",
            addedByUserId = "user-ana",
            updatedAt = 5L,
        )

        // When
        dataSource.applyRemote(listOf(serverPhoto))

        // Then
        assertEquals(
            "user-ana",
            dataSource
                .observeAttachments(MY_BILL)
                .first()
                .single()
                .addedByUserId,
        )
    }

    @Test
    fun `GIVEN photos of two accounts groups WHEN the server misses some THEN drops only the callers synced`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    attachment(id = "still-there"),
                    attachment(id = "gone"),
                    attachment(
                        id = "pending",
                        pendingJpeg = jpeg,
                        pendingSync = true,
                    ),
                    attachment(
                        id = "theirs",
                        expenseId = OTHER_USER_BILL,
                        groupId = OTHER_USER_GROUP,
                    ),
                ),
            )

            // When
            dataSource.deleteGoneFromServer(
                ownerUserId = USER_ID,
                serverAttachmentIds = setOf("still-there"),
            )

            // Then
            val remaining = listOf("still-there", "gone", "pending", "theirs").filter {
                database.billAttachmentDao().getAttachment(it) != null
            }
            assertEquals(listOf("still-there", "pending", "theirs"), remaining)
        }

    @Test
    fun `GIVEN pending photos of two users groups WHEN reading those of one THEN only its groups by addition`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    attachment(
                        id = "mine-late",
                        addedAt = 3L,
                        pendingSync = true,
                    ),
                    attachment(
                        id = "mine-early",
                        addedAt = 1L,
                        isDeleted = true,
                        pendingSync = true,
                    ),
                    attachment(id = "mine-synced"),
                    attachment(
                        id = "theirs",
                        expenseId = OTHER_USER_BILL,
                        groupId = OTHER_USER_GROUP,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            val pending = dataSource.getPendingAttachmentIds(ownerUserId = USER_ID)

            // Then
            assertEquals(listOf("mine-early", "mine-late"), pending)
        }

    @Test
    fun `GIVEN changes waiting to go up WHEN observing the pending versions THEN lists each id with its time`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    attachment(
                        id = "photo-2",
                        addedAt = 2L,
                        updatedAt = 7L,
                        isDeleted = true,
                        pendingSync = true,
                    ),
                    attachment(
                        id = "photo-1",
                        addedAt = 1L,
                        updatedAt = 4L,
                        pendingSync = true,
                    ),
                    attachment(id = "synced"),
                ),
            )

            // When
            val versions = dataSource.observePendingVersions().first()

            // Then
            assertEquals(listOf("photo-1:4", "photo-2:7"), versions)
        }

    @Test
    fun `GIVEN only synced photos WHEN observing pending changes THEN there are none`() = runTest {
        // Given
        prepareScenario(stored = listOf(attachment(id = "photo-1")))

        // When
        val hasPending = dataSource.observeHasPending().first()

        // Then
        assertFalse(hasPending)
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private fun group(
        id: String,
        ownerUserId: String,
    ): GroupEntity = GroupEntity(
        id = id,
        name = "Viagem Floripa",
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = ownerUserId,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = false,
        ownerUserId = ownerUserId,
        inviteCode = null,
        defaultSplit = null,
        splitIncomes = null,
    )

    private fun expense(
        id: String,
        groupId: String,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = groupId,
        createdBy = USER_ID,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = "equal",
        payersJson = "[]",
        sharesJson = "[]",
        itemsJson = null,
        categoryId = null,
        spentAt = 1_000L,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = 1L,
        updatedBy = null,
        pendingSync = false,
    )

    private fun attachment(
        id: String,
        expenseId: String = MY_BILL,
        groupId: String = MY_GROUP,
        kind: String = PHOTO,
        width: Int = 800,
        addedByUserId: String? = USER_ID,
        addedAt: Long = 1L,
        updatedAt: Long = 1L,
        isDeleted: Boolean = false,
        pendingJpeg: ByteArray? = null,
        pendingSync: Boolean = false,
    ): BillAttachmentEntity = BillAttachmentEntity(
        id = id,
        expenseId = expenseId,
        groupId = groupId,
        kind = kind,
        width = width,
        height = 600,
        addedByUserId = addedByUserId,
        addedByMemberId = "member-ana",
        addedAt = addedAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        pendingJpeg = pendingJpeg,
        pendingSync = pendingSync,
    )

    private suspend fun getPending(): List<BillAttachmentEntity> =
        dataSource.getPendingAttachmentIds(USER_ID).mapNotNull { dataSource.getAttachment(it) }

    private suspend fun prepareScenario(stored: List<BillAttachmentEntity> = emptyList()) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        database.groupDao().upsertGroup(
            group(
                id = MY_GROUP,
                ownerUserId = USER_ID,
            ),
        )
        database.groupDao().upsertGroup(
            group(
                id = OTHER_USER_GROUP,
                ownerUserId = "user-2",
            ),
        )
        database.expenseDao().insertExpense(
            expense(
                id = MY_BILL,
                groupId = MY_GROUP,
            ),
        )
        database.expenseDao().insertExpense(
            expense(
                id = OTHER_BILL,
                groupId = MY_GROUP,
            ),
        )
        database.expenseDao().insertExpense(
            expense(
                id = OTHER_USER_BILL,
                groupId = OTHER_USER_GROUP,
            ),
        )
        database.billAttachmentDao().upsertAttachments(stored)
        dataSource = RoomBillAttachmentLocalDataSource(database.billAttachmentDao())
    }

    private companion object {
        const val USER_ID = "user-1"
        const val MY_GROUP = "group-1"
        const val OTHER_USER_GROUP = "group-2"
        const val MY_BILL = "bill-1"
        const val OTHER_BILL = "bill-2"
        const val OTHER_USER_BILL = "bill-3"
        const val PHOTO = "photo"
        const val RECEIPT = "receipt"
    }
}
