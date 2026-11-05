package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.profile.data.datasource.AppInviteLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.AppInviteRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.dto.AppInviteDto
import com.quare.blitzsplit.core.profile.data.dto.PlatformSeenDto
import com.quare.blitzsplit.core.profile.data.repository.AppInviteRepositoryImpl
import com.quare.blitzsplit.core.profile.domain.model.AppInviteStatus
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.core.provider.room.entity.AppInviteEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppInviteRepositoryImplTest {
    private lateinit var local: FakeAppInviteLocalDataSource
    private lateinit var remote: FakeAppInviteRemoteDataSource
    private lateinit var repository: AppInviteRepositoryImpl

    @Test
    fun `GIVEN nothing stored WHEN observing the status THEN the invite is not dismissed and no phone app is in use`() =
        runTest {
            // Given
            prepareScenario(stored = null)

            // When
            val status = repository.observeStatus().first()

            // Then
            assertEquals(
                AppInviteStatus(
                    isDismissed = false,
                    phoneUse = null,
                ),
                status,
            )
        }

    @Test
    fun `GIVEN a dismissed invite WHEN observing the status THEN the invite is dismissed`() = runTest {
        // Given
        prepareScenario(stored = entity(dismissedAt = 42L))

        // When
        val status = repository.observeStatus().first()

        // Then
        assertTrue(status.isDismissed)
    }

    @Test
    fun `GIVEN the account seen only on Android WHEN observing the status THEN uses the phone since then on Android`() =
        runTest {
            // Given
            prepareScenario(stored = entity(androidSeenAt = 500L))

            // When
            val status = repository.observeStatus().first()

            // Then
            assertEquals(
                PhoneUse(
                    platforms = setOf(ClientPlatform.ANDROID),
                    sinceMillis = 500L,
                ),
                status.phoneUse,
            )
        }

    @Test
    fun `GIVEN the account seen on both phones WHEN observing the status THEN uses both since the earliest`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    androidSeenAt = 500L,
                    iosSeenAt = 300L,
                ),
            )

            // When
            val status = repository.observeStatus().first()

            // Then
            assertEquals(
                PhoneUse(
                    platforms = setOf(ClientPlatform.ANDROID, ClientPlatform.IOS),
                    sinceMillis = 300L,
                ),
                status.phoneUse,
            )
        }

    @Test
    fun `GIVEN nothing stored WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN a dismissal not on the server yet WHEN observing pending changes THEN reports it`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN a synced dismissal WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = false,
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN nothing stored and a profile of user 1 WHEN dismissing THEN stores it pending now for user 1`() =
        runTest {
            // Given
            prepareScenario(
                now = 1_000L,
                stored = null,
                profileOwner = "user-1",
            )

            // When
            repository.dismiss()

            // Then
            assertEquals(
                entity(
                    dismissedAt = 1_000L,
                    pendingSync = true,
                    ownerUserId = "user-1",
                ),
                local.row.value,
            )
            assertTrue(repository.observeStatus().first().isDismissed)
            assertTrue(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a local only profile WHEN dismissing THEN stores it pending without an owner`() = runTest {
        // Given
        prepareScenario(
            now = 1_000L,
            stored = null,
            profileOwner = null,
        )

        // When
        repository.dismiss()

        // Then
        assertEquals(
            entity(
                dismissedAt = 1_000L,
                pendingSync = true,
                ownerUserId = null,
            ),
            local.row.value,
        )
    }

    @Test
    fun `GIVEN no profile yet WHEN dismissing THEN stores it pending without an owner`() = runTest {
        // Given
        prepareScenario(
            now = 1_000L,
            stored = null,
            hasProfile = false,
        )

        // When
        repository.dismiss()

        // Then
        assertEquals(
            entity(
                dismissedAt = 1_000L,
                pendingSync = true,
                ownerUserId = null,
            ),
            local.row.value,
        )
    }

    @Test
    fun `GIVEN the account seen on Android WHEN dismissing THEN keeps the phone use and stores it pending`() = runTest {
        // Given
        prepareScenario(
            now = 1_000L,
            stored = entity(androidSeenAt = 500L),
        )

        // When
        repository.dismiss()

        // Then
        assertEquals(
            entity(
                dismissedAt = 1_000L,
                pendingSync = true,
                androidSeenAt = 500L,
            ),
            local.row.value,
        )
    }

    @Test
    fun `GIVEN an invite already dismissed WHEN dismissing again THEN the first time stays and nothing is written`() =
        runTest {
            // Given
            val stored = entity(
                dismissedAt = 42L,
                pendingSync = false,
            )
            prepareScenario(
                now = 1_000L,
                stored = stored,
            )

            // When
            repository.dismiss()

            // Then
            assertEquals(stored, local.row.value)
            assertTrue(local.upserts.isEmpty())
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the Android app WHEN syncing THEN records android with the server`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.ANDROID)

        // When
        repository.sync(userId = "user-1")

        // Then
        assertEquals(listOf("android"), remote.recordedPlatforms)
    }

    @Test
    fun `GIVEN the iPhone app WHEN syncing THEN records ios with the server`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.IOS)

        // When
        repository.sync(userId = "user-1")

        // Then
        assertEquals(listOf("ios"), remote.recordedPlatforms)
    }

    @Test
    fun `GIVEN the Mac app WHEN syncing THEN records macos with the server`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.MACOS)

        // When
        repository.sync(userId = "user-1")

        // Then
        assertEquals(listOf("macos"), remote.recordedPlatforms)
    }

    @Test
    fun `GIVEN the web app WHEN syncing THEN records web with the server`() = runTest {
        // Given
        prepareScenario(platform = ClientPlatform.WEB)

        // When
        repository.sync(userId = "user-1")

        // Then
        assertEquals(listOf("web"), remote.recordedPlatforms)
    }

    @Test
    fun `GIVEN the platform recorded for the user WHEN syncing again THEN records it once and reads the account`() =
        runTest {
            // Given
            prepareScenario(platform = ClientPlatform.WEB)
            repository.sync(userId = "user-1")

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("web"), remote.recordedPlatforms)
            assertEquals(2, remote.calls.count { it == "fetchPlatformsSeen:user-1" })
        }

    @Test
    fun `GIVEN the platform recorded for one user WHEN syncing under another THEN records it for that user too`() =
        runTest {
            // Given
            prepareScenario(platform = ClientPlatform.WEB)
            repository.sync(userId = "user-1")

            // When
            repository.sync(userId = "user-2")

            // Then
            assertEquals(listOf("web", "web"), remote.recordedPlatforms)
        }

    @Test
    fun `GIVEN nothing stored WHEN syncing THEN records the platform before fetching the account state`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("recordPlatformSeen", remote.calls.first())
        assertEquals(
            setOf("fetchDismissal:user-1", "fetchPlatformsSeen:user-1"),
            remote.calls.drop(1).toSet(),
        )
    }

    @Test
    fun `GIVEN nothing on the device nor on the server WHEN syncing THEN the invite stays open with nothing pending`() =
        runTest {
            // Given
            prepareScenario(stored = null)

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                AppInviteStatus(
                    isDismissed = false,
                    phoneUse = null,
                ),
                repository.observeStatus().first(),
            )
            assertFalse(repository.observeHasPendingChanges().first())
            assertTrue(remote.inserted.isEmpty())
        }

    @Test
    fun `GIVEN a dismissal only the server has WHEN syncing THEN it comes down synced for this user`() = runTest {
        // Given
        prepareScenario(
            stored = null,
            remoteDismissals = mapOf(
                "user-1" to AppInviteDto(
                    userId = "user-1",
                    dismissedAt = 77L,
                ),
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            entity(
                dismissedAt = 77L,
                pendingSync = false,
                ownerUserId = "user-1",
            ),
            local.row.value,
        )
        assertTrue(remote.inserted.isEmpty())
    }

    @Test
    fun `GIVEN a dismissal not on the server yet WHEN syncing THEN pushes it for this user and marks it synced`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    dismissedAt = 42L,
                    pendingSync = true,
                ),
            )

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    AppInviteDto(
                        userId = "user-1",
                        dismissedAt = 42L,
                    ),
                ),
                remote.inserted,
            )
            assertEquals(
                entity(
                    dismissedAt = 42L,
                    pendingSync = false,
                ),
                local.row.value,
            )
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a pending dismissal also on the server WHEN syncing THEN pushes nothing and ends pending`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            ),
            remoteDismissals = mapOf(
                "user-1" to AppInviteDto(
                    userId = "user-1",
                    dismissedAt = 10L,
                ),
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.inserted.isEmpty())
        assertTrue(repository.observeStatus().first().isDismissed)
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a synced dismissal the server no longer has WHEN syncing THEN the invite stays dismissed`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = false,
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(42L, local.row.value?.dismissedAt)
        assertTrue(repository.observeStatus().first().isDismissed)
        assertTrue(remote.inserted.isEmpty())
    }

    @Test
    fun `GIVEN a dismissal already pushed WHEN syncing again THEN pushes nothing more`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            ),
        )
        repository.sync(userId = "user-1")

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, remote.insertCalls)
        assertEquals(
            entity(
                dismissedAt = 42L,
                pendingSync = false,
            ),
            local.row.value,
        )
    }

    @Test
    fun `GIVEN the dismissal changed while the push was in flight WHEN syncing THEN the new one stays pending`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    dismissedAt = 42L,
                    pendingSync = true,
                ),
            )
            remote.onInsert = {
                local.row.value = entity(
                    dismissedAt = 43L,
                    pendingSync = true,
                )
            }

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(42L, remote.inserted.single().dismissedAt)
            assertEquals(
                entity(
                    dismissedAt = 43L,
                    pendingSync = true,
                ),
                local.row.value,
            )
            assertTrue(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the account seen on Android iPhone web and Mac WHEN syncing THEN stores the first time on each phone`() =
        runTest {
            // Given
            prepareScenario(
                stored = null,
                remotePlatformsSeen = mapOf(
                    "user-1" to listOf(
                        PlatformSeenDto(
                            platform = "web",
                            firstSeenAt = 10L,
                        ),
                        PlatformSeenDto(
                            platform = "macos",
                            firstSeenAt = 20L,
                        ),
                        PlatformSeenDto(
                            platform = "android",
                            firstSeenAt = 500L,
                        ),
                        PlatformSeenDto(
                            platform = "ios",
                            firstSeenAt = 300L,
                        ),
                    ),
                ),
            )

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(500L, local.row.value?.androidSeenAt)
            assertEquals(300L, local.row.value?.iosSeenAt)
            assertEquals(
                PhoneUse(
                    platforms = setOf(ClientPlatform.ANDROID, ClientPlatform.IOS),
                    sinceMillis = 300L,
                ),
                repository.observeStatus().first().phoneUse,
            )
        }

    @Test
    fun `GIVEN the account seen only on the web and the Mac WHEN syncing THEN no phone app is in use`() = runTest {
        // Given
        prepareScenario(
            stored = null,
            remotePlatformsSeen = mapOf(
                "user-1" to listOf(
                    PlatformSeenDto(
                        platform = "web",
                        firstSeenAt = 10L,
                    ),
                    PlatformSeenDto(
                        platform = "macos",
                        firstSeenAt = 20L,
                    ),
                ),
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.row.value?.androidSeenAt)
        assertNull(local.row.value?.iosSeenAt)
        assertNull(repository.observeStatus().first().phoneUse)
    }

    @Test
    fun `GIVEN a phone use the server no longer lists WHEN syncing THEN the phone use goes away`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                androidSeenAt = 500L,
                iosSeenAt = 300L,
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertNull(repository.observeStatus().first().phoneUse)
    }

    @Test
    fun `GIVEN a dismissal made while local only WHEN the first sync runs THEN user 1 claims it and pushes it`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    dismissedAt = 42L,
                    pendingSync = true,
                    ownerUserId = null,
                ),
            )

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    AppInviteDto(
                        userId = "user-1",
                        dismissedAt = 42L,
                    ),
                ),
                remote.inserted,
            )
            assertEquals(
                entity(
                    dismissedAt = 42L,
                    pendingSync = false,
                    ownerUserId = "user-1",
                ),
                local.row.value,
            )
        }

    @Test
    fun `GIVEN a pending dismissal of user 2 WHEN syncing under user 1 THEN drops it and pushes nothing`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
                ownerUserId = "user-2",
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.inserted.isEmpty())
        assertEquals("user-1", local.row.value?.ownerUserId)
        assertFalse(repository.observeStatus().first().isDismissed)
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN the row of user 2 WHEN syncing under user 1 THEN takes the server dismissal of user 1`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                androidSeenAt = 500L,
                ownerUserId = "user-2",
            ),
            remoteDismissals = mapOf(
                "user-1" to AppInviteDto(
                    userId = "user-1",
                    dismissedAt = 77L,
                ),
                "user-2" to AppInviteDto(
                    userId = "user-2",
                    dismissedAt = 99L,
                ),
            ),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            entity(
                dismissedAt = 77L,
                pendingSync = false,
                ownerUserId = "user-1",
            ),
            local.row.value,
        )
    }

    @Test
    fun `GIVEN the server fails to record the platform WHEN syncing THEN fails and keeps the dismissal pending`() =
        runTest {
            // Given
            val stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            )
            prepareScenario(
                stored = stored,
                recordError = IllegalStateException("offline"),
            )

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(remote.inserted.isEmpty())
            assertEquals(stored, local.row.value)
            assertTrue(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the server fails to fetch the dismissal WHEN syncing THEN fails and keeps the dismissal pending`() =
        runTest {
            // Given
            val stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            )
            prepareScenario(
                stored = stored,
                fetchDismissalError = IllegalStateException("offline"),
            )

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(remote.inserted.isEmpty())
            assertEquals(stored, local.row.value)
        }

    @Test
    fun `GIVEN the server fails to fetch the platforms seen WHEN syncing THEN fails and keeps the dismissal pending`() =
        runTest {
            // Given
            val stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            )
            prepareScenario(
                stored = stored,
                fetchPlatformsError = IllegalStateException("offline"),
            )

            // When
            val result = repository.sync(userId = "user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(remote.inserted.isEmpty())
            assertEquals(stored, local.row.value)
        }

    @Test
    fun `GIVEN the server fails to take the dismissal WHEN syncing THEN fails and keeps it pending`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
            ),
            insertError = IllegalStateException("offline"),
        )

        // When
        val result = repository.sync(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(42L, local.row.value?.dismissedAt)
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a dismissed invite and a phone use WHEN clearing THEN leaves nothing of the account`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                dismissedAt = 42L,
                pendingSync = true,
                androidSeenAt = 500L,
            ),
        )

        // When
        repository.clear()

        // Then
        assertNull(local.row.value)
        assertEquals(
            AppInviteStatus(
                isDismissed = false,
                phoneUse = null,
            ),
            repository.observeStatus().first(),
        )
        assertFalse(repository.observeHasPendingChanges().first())
    }

    private fun entity(
        dismissedAt: Long? = null,
        pendingSync: Boolean = false,
        androidSeenAt: Long? = null,
        iosSeenAt: Long? = null,
        ownerUserId: String? = "user-1",
    ): AppInviteEntity = AppInviteEntity(
        id = SINGLE_ROW_ID,
        dismissedAt = dismissedAt,
        pendingSync = pendingSync,
        androidSeenAt = androidSeenAt,
        iosSeenAt = iosSeenAt,
        ownerUserId = ownerUserId,
    )

    private fun prepareScenario(
        now: Long = 0L,
        stored: AppInviteEntity? = null,
        hasProfile: Boolean = true,
        profileOwner: String? = "user-1",
        platform: ClientPlatform = ClientPlatform.WEB,
        remoteDismissals: Map<String, AppInviteDto> = emptyMap(),
        remotePlatformsSeen: Map<String, List<PlatformSeenDto>> = emptyMap(),
        recordError: Throwable? = null,
        fetchDismissalError: Throwable? = null,
        fetchPlatformsError: Throwable? = null,
        insertError: Throwable? = null,
    ) {
        local = FakeAppInviteLocalDataSource(stored)
        remote = FakeAppInviteRemoteDataSource(
            dismissals = remoteDismissals.toMutableMap(),
            platformsSeen = remotePlatformsSeen,
            recordError = recordError,
            fetchDismissalError = fetchDismissalError,
            fetchPlatformsError = fetchPlatformsError,
            insertError = insertError,
        )
        repository = AppInviteRepositoryImpl(
            local = local,
            remote = remote,
            profileLocal = FakeAppInviteOwnerProfileLocalDataSource(
                profile = if (hasProfile) {
                    ProfileEntity(
                        id = SINGLE_ROW_ID,
                        displayName = "Pierre",
                        avatarColor = "lilac",
                        photoUrl = null,
                        updatedAt = 1L,
                        pendingSync = false,
                        pendingPhoto = null,
                        ownerUserId = profileOwner,
                    )
                } else {
                    null
                },
            ),
            platform = platform,
            currentTimeMillis = { now },
        )
    }
}

// Why: the guards mirror AppInviteDao's queries, so the repository is checked against the same rules.
private class FakeAppInviteLocalDataSource(
    stored: AppInviteEntity?,
) : AppInviteLocalDataSource {
    val row = MutableStateFlow(stored)
    val upserts = mutableListOf<AppInviteEntity>()

    override fun observe(): Flow<AppInviteEntity?> = row

    override suspend fun get(): AppInviteEntity? {
        val stored = row.value
        yield()
        return stored
    }

    override suspend fun upsert(entity: AppInviteEntity) {
        upserts += entity
        row.value = entity
    }

    override suspend fun markSynced(syncedDismissedAt: Long) {
        row.value = row.value?.let { if (it.dismissedAt == syncedDismissedAt) it.copy(pendingSync = false) else it }
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        row.value = row.value?.let { if (it.ownerUserId == null) it.copy(ownerUserId = ownerUserId) else it }
    }

    override suspend fun deleteOtherUsers(ownerUserId: String) {
        val owner = row.value?.ownerUserId
        if (owner != null && owner != ownerUserId) row.value = null
    }

    override suspend fun clear() {
        row.value = null
    }
}

// Why: the server's rows per user; an insert of a dismissal already there keeps the first, as the real insert does.
private class FakeAppInviteRemoteDataSource(
    private val dismissals: MutableMap<String, AppInviteDto>,
    private val platformsSeen: Map<String, List<PlatformSeenDto>>,
    private val recordError: Throwable?,
    private val fetchDismissalError: Throwable?,
    private val fetchPlatformsError: Throwable?,
    private val insertError: Throwable?,
) : AppInviteRemoteDataSource {
    val calls = mutableListOf<String>()
    val recordedPlatforms = mutableListOf<String>()
    val inserted = mutableListOf<AppInviteDto>()
    var insertCalls = 0
    var onInsert: () -> Unit = {}

    override suspend fun fetchDismissal(userId: String): AppInviteDto? {
        calls += "fetchDismissal:$userId"
        fetchDismissalError?.let { throw it }
        return dismissals[userId]
    }

    override suspend fun insertDismissal(dismissal: AppInviteDto) {
        calls += "insertDismissal"
        insertCalls++
        onInsert()
        insertError?.let { throw it }
        inserted += dismissal
        if (dismissal.userId !in dismissals) dismissals[dismissal.userId] = dismissal
    }

    override suspend fun fetchPlatformsSeen(userId: String): List<PlatformSeenDto> {
        calls += "fetchPlatformsSeen:$userId"
        fetchPlatformsError?.let { throw it }
        return platformsSeen[userId].orEmpty()
    }

    override suspend fun recordPlatformSeen(platform: String) {
        calls += "recordPlatformSeen"
        recordError?.let { throw it }
        recordedPlatforms += platform
    }
}

private class FakeAppInviteOwnerProfileLocalDataSource(
    private val profile: ProfileEntity?,
) : ProfileLocalDataSource {
    override fun observe(): Flow<ProfileEntity?> = error("unused")

    override suspend fun get(): ProfileEntity? = profile

    override suspend fun upsert(entity: ProfileEntity) = error("unused")

    override suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    ) = error("unused")

    override suspend fun applyRemote(entity: ProfileEntity) = error("unused")

    override suspend fun claimUnowned(ownerUserId: String) = error("unused")

    override suspend fun replaceOtherUsersCopy(entity: ProfileEntity) = error("unused")

    override suspend fun reclaimOtherUsersCopy(
        ownerUserId: String,
        updatedAt: Long,
        photoUrl: String?,
    ) = error("unused")

    override suspend fun clear() = error("unused")
}
