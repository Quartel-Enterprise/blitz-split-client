package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.groups.testing.FakeInviteRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.usecase.impl.JoinGroupUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JoinGroupUseCaseTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var invites: FakeInviteRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var useCase: JoinGroupUseCase

    @Test
    fun `GIVEN no account WHEN joining THEN signs in anonymously before joining`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID), account.status)
        assertEquals(1, invites.joins.size)
        assertEquals(Result.success(joined), result)
    }

    @Test
    fun `GIVEN no account and no internet WHEN joining THEN fails without trying to join`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            registerError = IllegalStateException("offline"),
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(invites.joins.isEmpty())
        assertTrue(profile.savedProfiles.isEmpty())
        assertTrue(groups.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN an anonymous account WHEN joining THEN joins as the same user`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.RESTORED_USER_ID))

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(AccountStatus.Anonymous(FakeAccountRepository.RESTORED_USER_ID), account.status)
        assertEquals(1, invites.joins.size)
        assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.downloadedUserIds)
        assertEquals(Result.success(joined), result)
    }

    @Test
    fun `GIVEN a Google account WHEN joining THEN joins as that account`() = runTest {
        // Given
        val connected = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
        prepareScenario(status = connected)

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(connected, account.status)
        assertEquals(1, invites.joins.size)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), groups.downloadedUserIds)
        assertEquals(Result.success(joined), result)
    }

    @Test
    fun `GIVEN the invite code WHEN joining as a member THEN joins with that code and that member`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(invite, asAna)

        // Then
        assertEquals(invite.code, invites.joins.single().code)
        assertEquals(asAna, invites.joins.single().choice)
    }

    @Test
    fun `GIVEN a profile on the device WHEN joining as a member THEN sends the color of the profile`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        useCase(invite, asAna)

        // Then
        assertEquals(pierre.avatarColor, invites.joins.single().avatarColor)
    }

    @Test
    fun `GIVEN a profile on the device WHEN joining under a new name THEN sends the color of the profile`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        useCase(invite, asNewMember)

        // Then
        assertEquals(pierre.avatarColor, invites.joins.single().avatarColor)
    }

    @Test
    fun `GIVEN no profile and a member of one letter WHEN joining as them THEN fails without trying to join`() =
        runTest {
            // Given
            prepareScenario(storedProfile = null)
            val withJ = invite.copy(
                members =
                    invite.members + InviteMember("j", "J", AvatarColor.SAND, hasJoined = false),
            )

            // When
            val result = useCase(withJ, JoinChoice.AsMember("j"))

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertTrue(invites.joins.isEmpty())
            assertTrue(profile.savedProfiles.isEmpty())
        }

    @Test
    fun `GIVEN no profile WHEN joining as a member THEN sends the color of that member`() = runTest {
        // Given
        prepareScenario(storedProfile = null)

        // When
        useCase(invite, asAna)

        // Then
        assertEquals(ana.avatarColor, invites.joins.single().avatarColor)
    }

    @Test
    fun `GIVEN no profile WHEN joining under a new name THEN sends the color fewest members have`() = runTest {
        // Given
        prepareScenario(storedProfile = null)

        // When
        useCase(invite, asNewMember)

        // Then
        assertEquals(AvatarColor.SAND, invites.joins.single().avatarColor)
    }

    @Test
    fun `GIVEN no profile WHEN joined as a member THEN saves a profile with the name and color of the member`() =
        runTest {
            // Given
            prepareScenario(storedProfile = null)

            // When
            useCase(invite, asAna)

            // Then
            val saved = UserProfile(
                displayName = ana.name,
                avatarColor = ana.avatarColor,
                photo = null,
            )
            assertEquals(listOf(saved), profile.savedProfiles)
            assertEquals(saved, profile.profile.value)
        }

    @Test
    fun `GIVEN no profile WHEN joined under a new name THEN saves a profile with the trimmed name and its color`() =
        runTest {
            // Given
            prepareScenario(storedProfile = null)

            // When
            useCase(invite, asNewMember)

            // Then
            val saved = UserProfile(
                displayName = "Bruno",
                avatarColor = AvatarColor.SAND,
                photo = null,
            )
            assertEquals(listOf(saved), profile.savedProfiles)
        }

    @Test
    fun `GIVEN no profile and already a member WHEN joining THEN saves a profile from the group`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            joinOutcome = alreadyMember,
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        val saved = UserProfile(
            displayName = ana.name,
            avatarColor = ana.avatarColor,
            photo = null,
        )
        assertEquals(listOf(saved), profile.savedProfiles)
        assertEquals(Result.success(alreadyMember), result)
    }

    @Test
    fun `GIVEN a profile on the device WHEN joined THEN keeps the profile untouched`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        useCase(invite, asNewMember)

        // Then
        assertTrue(profile.savedProfiles.isEmpty())
        assertEquals(pierre, profile.profile.value)
    }

    @Test
    fun `GIVEN no profile WHEN joined THEN syncs the group to the device before returning`() = runTest {
        // Given
        prepareScenario(storedProfile = null)

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
        assertEquals(Result.success(joined), result)
    }

    @Test
    fun `GIVEN a profile on the device and already a member WHEN joining THEN syncs the group to the device`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                joinOutcome = alreadyMember,
            )

            // When
            val result = useCase(invite, asAna)

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
            assertEquals(Result.success(alreadyMember), result)
        }

    @Test
    fun `GIVEN the sync fails WHEN joined THEN fails`() = runTest {
        // Given
        prepareScenario(groupPullError = IllegalStateException("sync failed"))

        // When
        val result = useCase(invite, asAna)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN someone else took the member WHEN joining THEN answers member taken without a profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            joinOutcome = JoinOutcome.MemberTaken,
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(Result.success(JoinOutcome.MemberTaken), result)
        assertTrue(profile.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN the member was removed WHEN joining THEN answers member gone without a profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            joinOutcome = JoinOutcome.MemberGone,
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(Result.success(JoinOutcome.MemberGone), result)
        assertTrue(profile.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN a member already has the name WHEN joining under it THEN answers name taken without a profile`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                joinOutcome = JoinOutcome.NameTaken,
            )

            // When
            val result = useCase(invite, asNewMember)

            // Then
            assertEquals(Result.success(JoinOutcome.NameTaken), result)
            assertTrue(profile.savedProfiles.isEmpty())
        }

    @Test
    fun `GIVEN the link was changed WHEN joining THEN answers invalid link without a profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            joinOutcome = JoinOutcome.InvalidLink,
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        assertEquals(Result.success(JoinOutcome.InvalidLink), result)
        assertTrue(profile.savedProfiles.isEmpty())
    }

    @Test
    fun `GIVEN a profile on the device and the member taken WHEN joining THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            joinOutcome = JoinOutcome.MemberTaken,
        )

        // When
        useCase(invite, asAna)

        // Then
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a profile on the device and the link changed WHEN joining THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            joinOutcome = JoinOutcome.InvalidLink,
        )

        // When
        useCase(invite, asAna)

        // Then
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the server fails to join WHEN joining THEN fails without saving or syncing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            joinError = IllegalStateException("server error"),
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(profile.savedProfiles.isEmpty())
        assertTrue(groups.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a profile on the device and the server fails to join WHEN joining THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            joinError = IllegalStateException("server error"),
        )

        // When
        val result = useCase(invite, asAna)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(groups.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the join is cancelled WHEN joining THEN the cancellation propagates`() = runTest {
        // Given
        prepareScenario(joinError = CancellationException("left the screen"))

        // When
        val error = assertFailsWith<CancellationException> { useCase(invite, asAna) }

        // Then
        assertEquals("left the screen", error.message)
        assertTrue(profile.savedProfiles.isEmpty())
    }

    private val ana = InviteMember(
        id = "ana",
        name = "Ana",
        avatarColor = AvatarColor.PEACH,
        hasJoined = false,
    )

    // Why: every color but SAND is taken, BLUE twice, so the least used one isn't the first of the palette.
    private val invite = Invite(
        code = "K7QM4XPA",
        groupId = "group-1",
        groupName = "Viagem",
        groupType = GroupType.TRIP,
        groupColor = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        creatorName = "Carla",
        isMember = false,
        members = listOf(
            InviteMember(
                id = "carla",
                name = "Carla",
                avatarColor = AvatarColor.BLUE,
                hasJoined = true,
            ),
            ana,
            InviteMember(
                id = "duda",
                name = "Duda",
                avatarColor = AvatarColor.GREEN,
                hasJoined = false,
            ),
            InviteMember(
                id = "eva",
                name = "Eva",
                avatarColor = AvatarColor.LILAC,
                hasJoined = false,
            ),
            InviteMember(
                id = "fabi",
                name = "Fabi",
                avatarColor = AvatarColor.BLUE,
                hasJoined = false,
            ),
        ),
        personalMemberId = null,
        inviterName = null,
    )
    private val asAna = JoinChoice.AsMember(ana.id)
    private val asNewMember = JoinChoice.AsNewMember("  Bruno ")
    private val joined = JoinOutcome.Joined(invite.groupId)
    private val alreadyMember = JoinOutcome.AlreadyMember(invite.groupId, memberId = ana.id)
    private val pierre = UserProfile("Pierre", AvatarColor.LILAC, photo = null)

    private fun TestScope.prepareScenario(
        status: AccountStatus = AccountStatus.LocalOnly,
        storedProfile: UserProfile? = null,
        registerError: Throwable? = null,
        joinOutcome: JoinOutcome = joined,
        joinError: Throwable? = null,
        groupPullError: Throwable? = null,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.registerError = registerError }
        profile = FakeProfileRepository(initialProfile = storedProfile)
        invites = FakeInviteRepository(
            invite = invite,
            joinOutcome = joinOutcome,
        ).also { it.joinError = joinError }
        groups = FakeGroupRepository().also { it.pullError = groupPullError }
        useCase = JoinGroupUseCase(
            accountRepository = account,
            profileRepository = profile,
            inviteRepository = invites,
            coordinator = SyncCoordinator(
                accountRepository = account,
                profileRepository = profile,
                paymentKeyRepository = FakePaymentKeyRepository(),
                notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
                groupRepository = groups,
                groupPinRepository = FakeGroupPinRepository(),
                expenseRepository = FakeExpenseRepository(),
                settlementRepository = FakeSettlementRepository(),
                categoryRepository = FakeCategoryRepository(),
                categoryTermRepository = FakeCategoryTermRepository(),
                activityRepository = FakeActivityRepository(),
                hiddenContactRepository = FakeHiddenContactRepository(),
                firstDownload = FirstDownload(),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                scope = backgroundScope,
            ),
        )
    }
}
