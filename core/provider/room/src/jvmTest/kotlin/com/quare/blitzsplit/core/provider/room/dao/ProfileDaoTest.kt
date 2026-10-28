package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Why: the rules of a switch live in the SQL guards, so they run against SQLite rather than a fake.
class ProfileDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ProfileDao

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN another user copy not pushed yet WHEN replacing it with an older server profile THEN the server wins`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    updatedAt = 50L,
                    pendingSync = true,
                    pendingPhoto = jpeg,
                ),
            )

            // When
            dao.replaceOtherUsersCopy(
                displayName = "Larissa",
                avatarColor = "peach",
                photoUrl = null,
                remoteUpdatedAt = 10L,
                ownerUserId = "user-2",
            )

            // Then
            assertEquals(
                entity(
                    displayName = "Larissa",
                    avatarColor = "peach",
                    updatedAt = 10L,
                    pendingSync = false,
                    ownerUserId = "user-2",
                ),
                dao.get(),
            )
        }

    @Test
    fun `GIVEN a copy of this user WHEN replacing it as another user copy THEN leaves it`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = true))

        // When
        dao.replaceOtherUsersCopy(
            displayName = "Larissa",
            avatarColor = "peach",
            photoUrl = null,
            remoteUpdatedAt = 99L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals("Pierre", dao.get()?.displayName)
    }

    @Test
    fun `GIVEN another user copy WHEN applying a newer server profile of this user THEN leaves it`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        dao.applyRemote(
            displayName = "Larissa",
            avatarColor = "peach",
            photoUrl = null,
            remoteUpdatedAt = 99L,
            ownerUserId = "user-2",
        )

        // Then
        assertEquals("Pierre", dao.get()?.displayName)
    }

    @Test
    fun `GIVEN a synced copy of this user WHEN applying a newer server profile THEN applies it`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingSync = false))

        // When
        dao.applyRemote(
            displayName = "Larissa",
            avatarColor = "peach",
            photoUrl = null,
            remoteUpdatedAt = 99L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals("Larissa", dao.get()?.displayName)
        assertEquals(99L, dao.get()?.updatedAt)
    }

    @Test
    fun `GIVEN a change of this user not pushed yet WHEN applying a newer server profile THEN keeps the change`() =
        runTest {
            // Given
            prepareScenario(stored = entity(pendingSync = true))

            // When
            dao.applyRemote(
                displayName = "Larissa",
                avatarColor = "peach",
                photoUrl = null,
                remoteUpdatedAt = 99L,
                ownerUserId = "user-1",
            )

            // Then
            assertEquals("Pierre", dao.get()?.displayName)
        }

    @Test
    fun `GIVEN a profile made before its account WHEN claiming it THEN it becomes this user copy`() = runTest {
        // Given
        prepareScenario(stored = entity(ownerUserId = null))

        // When
        dao.claimUnowned("user-1")

        // Then
        assertEquals("user-1", dao.get()?.ownerUserId)
    }

    @Test
    fun `GIVEN another user copy WHEN claiming it THEN it stays that user copy`() = runTest {
        // Given
        prepareScenario(stored = entity())

        // When
        dao.claimUnowned("user-2")

        // Then
        assertEquals("user-1", dao.get()?.ownerUserId)
    }

    @Test
    fun `GIVEN another user copy WHEN reclaiming it THEN it waits to go up as this user copy`() = runTest {
        // Given
        prepareScenario(stored = entity(photoUrl = "https://storage/avatars/user-1/avatar.jpg"))

        // When
        dao.reclaimOtherUsersCopy(
            ownerUserId = "user-2",
            updatedAt = 2_000L,
            photoUrl = null,
        )

        // Then
        val stored = dao.get()
        assertEquals(true, stored?.pendingSync)
        assertEquals(2_000L, stored?.updatedAt)
        assertNull(stored?.photoUrl)
        assertEquals("user-2", stored?.ownerUserId)
    }

    @Test
    fun `GIVEN a copy of this user WHEN reclaiming it THEN leaves it`() = runTest {
        // Given
        prepareScenario(stored = entity(pendingPhoto = jpeg))

        // When
        dao.reclaimOtherUsersCopy(
            ownerUserId = "user-1",
            updatedAt = 2_000L,
            photoUrl = null,
        )

        // Then
        val stored = dao.get()
        assertEquals(false, stored?.pendingSync)
        assertContentEquals(jpeg, stored?.pendingPhoto)
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private fun entity(
        displayName: String = "Pierre",
        avatarColor: String = "lilac",
        photoUrl: String? = null,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        pendingPhoto: ByteArray? = null,
        ownerUserId: String? = "user-1",
    ): ProfileEntity = ProfileEntity(
        id = SINGLE_ROW_ID,
        displayName = displayName,
        avatarColor = avatarColor,
        photoUrl = photoUrl,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        pendingPhoto = pendingPhoto,
        ownerUserId = ownerUserId,
    )

    private suspend fun prepareScenario(stored: ProfileEntity) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        dao = database.profileDao()
        dao.upsert(stored)
    }
}
