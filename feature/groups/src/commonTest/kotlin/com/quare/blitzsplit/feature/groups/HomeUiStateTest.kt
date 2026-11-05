package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.feature.groups.presentation.model.NotificationPrompt
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeUiStateTest {
    @Test
    fun `GIVEN the groups not read yet WHEN Inicio draws THEN shows neither the home the first use nor Rachar conta`() {
        // Given
        val state = state(isLoaded = false)

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(false, false, false), shown)
    }

    @Test
    fun `GIVEN no group read from the device WHEN Inicio draws THEN shows only the first use`() {
        // Given
        val state = state(groupCount = 0)

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(false, true, false), shown)
    }

    @Test
    fun `GIVEN a first download before the groups are read WHEN Inicio draws THEN shows the home not the first use`() {
        // Given
        val state = state(
            isLoaded = false,
            isFirstDownload = true,
        )

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(true, false, false), shown)
    }

    @Test
    fun `GIVEN a first download with no group yet WHEN Inicio draws THEN shows the home without the first use`() {
        // Given
        val state = state(
            groupCount = 0,
            isFirstDownload = true,
        )

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(true, false, false), shown)
    }

    @Test
    fun `GIVEN a first download with groups already down WHEN Inicio draws THEN shows the home not the first use`() {
        // Given
        val state = state(groupCount = 3).copy(
            firstDownload = DownloadProgress(
                arrivedGroupCount = 3,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
        )

        // When
        val shown = listOf(state.isFirstDownload, state.showsHome, state.showsFirstUse)

        // Then
        assertEquals(listOf(true, true, false), shown)
    }

    @Test
    fun `GIVEN no first download WHEN Inicio draws THEN it is no first download`() {
        // Given
        val state = state(groupCount = 0)

        // When
        val isFirstDownload = state.isFirstDownload

        // Then
        assertFalse(isFirstDownload)
    }

    @Test
    fun `GIVEN a group on the device WHEN Inicio draws THEN shows the home and Rachar conta`() {
        // Given
        val state = state(groupCount = 1)

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(true, false, true), shown)
    }

    @Test
    fun `GIVEN everyone settled WHEN Inicio draws THEN hides the faces`() {
        // Given
        val state = state(people = emptyList())

        // When
        val showsPeople = state.showsPeople

        // Then
        assertFalse(showsPeople)
    }

    @Test
    fun `GIVEN someone with an open balance WHEN Inicio draws THEN shows the faces`() {
        // Given
        val state = state(
            people = listOf(
                HomePerson(
                    key = "name:ana",
                    name = "Ana",
                    initials = "AN",
                    color = AvatarColor.PEACH,
                    photoUrl = null,
                    currency = GroupCurrency.BRL,
                    netMinor = -3000L,
                    isGuest = false,
                ),
            ),
        )

        // When
        val showsPeople = state.showsPeople

        // Then
        assertTrue(showsPeople)
    }

    @Test
    fun `GIVEN no internet WHEN Inicio draws THEN Ler cupom fiscal is disabled`() {
        // Given
        val state = state(isOnline = false)

        // When
        val canScanReceipt = state.canScanReceipt

        // Then
        assertFalse(canScanReceipt)
    }

    @Test
    fun `GIVEN internet WHEN Inicio draws THEN Ler cupom fiscal is enabled`() {
        // Given
        val state = state(isOnline = true)

        // When
        val canScanReceipt = state.canScanReceipt

        // Then
        assertTrue(canScanReceipt)
    }

    @Test
    fun `GIVEN each sync badge WHEN Inicio draws THEN only the failed one can be retried`() {
        // Given
        val badges = listOf(null, SyncBadge.OFFLINE, SyncBadge.SYNCING, SyncBadge.FAILED)

        // When
        val retryable = badges.map { state(syncBadge = it).canRetrySync }

        // Then
        assertEquals(listOf(false, false, false, true), retryable)
    }

    @Test
    fun `GIVEN one person you owe and one who owes you WHEN Inicio draws THEN Voce deve settles up with that one`() {
        // Given
        val bruno = person(
            key = "name:bruno",
            netMinor = -12_600L,
        )
        val state = state(
            people = listOf(
                bruno,
                person(
                    key = "name:diego",
                    netMinor = 4500L,
                ),
            ),
        )

        // When
        val onlyPersonToPay = state.onlyPersonToPay

        // Then
        assertEquals(bruno, onlyPersonToPay)
    }

    @Test
    fun `GIVEN two people you owe WHEN Inicio draws THEN Voce deve opens Saldos instead`() {
        // Given
        val state = state(
            people = listOf(
                person(
                    key = "name:bruno",
                    netMinor = -12_600L,
                ),
                person(
                    key = "name:carla",
                    netMinor = -3000L,
                ),
            ),
        )

        // When
        val onlyPersonToPay = state.onlyPersonToPay

        // Then
        assertNull(onlyPersonToPay)
    }

    @Test
    fun `GIVEN Saldos com pessoas not read yet WHEN Inicio draws THEN shows no faces`() {
        // Given
        val state = state(peopleRead = false)

        // When
        val showsPeople = state.showsPeople

        // Then
        assertFalse(showsPeople)
    }

    @Test
    fun `GIVEN Saldos com pessoas not read yet WHEN Inicio draws THEN Voce deve settles up with nobody`() {
        // Given
        val state = state(peopleRead = false)

        // When
        val onlyPersonToPay = state.onlyPersonToPay

        // Then
        assertNull(onlyPersonToPay)
    }

    @Test
    fun `GIVEN a group and each notification permission WHEN Inicio draws THEN picks the card that permission needs`() {
        // Given
        val permissions = listOf(
            NotificationPermission.NOT_DETERMINED,
            NotificationPermission.REFUSED_ONCE,
            NotificationPermission.DENIED,
            NotificationPermission.GRANTED,
            null,
        )

        // When
        val prompts = permissions.map { state(notificationPermission = it).notificationPrompt }

        // Then
        assertEquals(
            listOf(NotificationPrompt.SOFT_ASK, NotificationPrompt.ASK_AGAIN, NotificationPrompt.BLOCKED, null, null),
            prompts,
        )
    }

    @Test
    fun `GIVEN the first use with no group WHEN notifications were never asked THEN shows no notification card`() {
        // Given
        val state = state(
            groupCount = 0,
            notificationPermission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN the groups not read yet WHEN notifications were never asked THEN shows no notification card`() {
        // Given
        val state = state(
            isLoaded = false,
            notificationPermission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN a first download with a group already down WHEN notifications were never asked THEN shows no card`() {
        // Given
        val state = state(
            isFirstDownload = true,
            notificationPermission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN the recovery code card WHEN notifications were never asked THEN shows no notification card`() {
        // Given
        val state = state(
            showsRecoveryCodeCard = true,
            notificationPermission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN the recovery code sheet WHEN notifications were never asked THEN shows no notification card`() {
        // Given
        val state = state(
            showsRecoveryCodePrompt = true,
            notificationPermission = NotificationPermission.NOT_DETERMINED,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN the card snoozed WHEN notifications are blocked THEN shows no notification card`() {
        // Given
        val state = state(
            isNotificationPromptSnoozed = true,
            notificationPermission = NotificationPermission.DENIED,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN Apple never asked and not asked quietly yet WHEN Inicio draws THEN shows no notification card`() {
        // Given
        val state = state(
            notificationPermission = NotificationPermission.NOT_DETERMINED,
            canAskQuietly = true,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertNull(prompt)
    }

    @Test
    fun `GIVEN Apple still never asked after the quiet request WHEN Inicio draws THEN falls back to the soft ask`() {
        // Given
        val state = state(
            notificationPermission = NotificationPermission.NOT_DETERMINED,
            canAskQuietly = true,
            hasAskedQuietly = true,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertEquals(NotificationPrompt.SOFT_ASK, prompt)
    }

    @Test
    fun `GIVEN Android or the web never asked WHEN Inicio draws THEN shows the soft ask with no quiet request`() {
        // Given
        val state = state(
            notificationPermission = NotificationPermission.NOT_DETERMINED,
            canAskQuietly = false,
        )

        // When
        val drawn = state.notificationPrompt to state.shouldAskQuietly

        // Then
        assertEquals(NotificationPrompt.SOFT_ASK to false, drawn)
    }

    @Test
    fun `GIVEN a group and quiet delivery on Apple WHEN Inicio draws THEN shows the quiet card`() {
        // Given
        val state = state(
            notificationPermission = NotificationPermission.PROVISIONAL,
            canAskQuietly = true,
            hasAskedQuietly = true,
        )

        // When
        val prompt = state.notificationPrompt

        // Then
        assertEquals(NotificationPrompt.QUIET, prompt)
    }

    @Test
    fun `GIVEN quiet delivery and each reason the other cards hide WHEN Inicio draws THEN shows no quiet card`() {
        // Given
        val states = listOf(
            state(groupCount = 0),
            state(isLoaded = false),
            state(isFirstDownload = true),
            state(showsRecoveryCodeCard = true),
            state(showsRecoveryCodePrompt = true),
            state(isNotificationPromptSnoozed = true),
        ).map {
            it.copy(
                notificationPermission = NotificationPermission.PROVISIONAL,
                canAskQuietly = true,
                hasAskedQuietly = true,
            )
        }

        // When
        val prompts = states.map { it.notificationPrompt }

        // Then
        assertEquals(List(states.size) { null }, prompts)
    }

    @Test
    fun `GIVEN a group and Apple never asked WHEN Inicio draws THEN asks quietly`() {
        // Given
        val state = state(
            notificationPermission = NotificationPermission.NOT_DETERMINED,
            canAskQuietly = true,
        )

        // When
        val shouldAsk = state.shouldAskQuietly

        // Then
        assertTrue(shouldAsk)
    }

    @Test
    fun `GIVEN Apple never asked and each thing that hides the card WHEN Inicio draws THEN still asks quietly`() {
        // Given
        val states = listOf(
            state(isFirstDownload = true),
            state(showsRecoveryCodeCard = true),
            state(showsRecoveryCodePrompt = true),
            state(isNotificationPromptSnoozed = true),
        ).map {
            it.copy(
                notificationPermission = NotificationPermission.NOT_DETERMINED,
                canAskQuietly = true,
            )
        }

        // When
        val asks = states.map { it.shouldAskQuietly }

        // Then
        assertEquals(List(states.size) { true }, asks)
    }

    @Test
    fun `GIVEN Apple never asked with no group yet WHEN Inicio draws THEN does not ask quietly`() {
        // Given
        val states = listOf(state(groupCount = 0), state(isLoaded = false)).map {
            it.copy(
                notificationPermission = NotificationPermission.NOT_DETERMINED,
                canAskQuietly = true,
            )
        }

        // When
        val asks = states.map { it.shouldAskQuietly }

        // Then
        assertEquals(listOf(false, false), asks)
    }

    @Test
    fun `GIVEN Apple already asked quietly WHEN Inicio draws THEN does not ask quietly again`() {
        // Given
        val state = state(
            notificationPermission = NotificationPermission.NOT_DETERMINED,
            canAskQuietly = true,
            hasAskedQuietly = true,
        )

        // When
        val shouldAsk = state.shouldAskQuietly

        // Then
        assertFalse(shouldAsk)
    }

    @Test
    fun `GIVEN a group on Apple and each permission but never asked WHEN Inicio draws THEN does not ask quietly`() {
        // Given
        val permissions = listOf(
            NotificationPermission.PROVISIONAL,
            NotificationPermission.GRANTED,
            NotificationPermission.DENIED,
            NotificationPermission.REFUSED_ONCE,
            null,
        )

        // When
        val asks = permissions.map {
            state(
                notificationPermission = it,
                canAskQuietly = true,
            ).shouldAskQuietly
        }

        // Then
        assertEquals(List(permissions.size) { false }, asks)
    }

    private fun person(
        key: String,
        netMinor: Long,
    ): HomePerson = HomePerson(
        key = key,
        name = key.removePrefix("name:"),
        initials = key.removePrefix("name:").take(2).uppercase(),
        color = AvatarColor.PEACH,
        photoUrl = null,
        currency = GroupCurrency.BRL,
        netMinor = netMinor,
        isGuest = false,
    )

    private fun state(
        isLoaded: Boolean = true,
        groupCount: Int = 1,
        isFirstDownload: Boolean = false,
        isOnline: Boolean = true,
        syncBadge: SyncBadge? = null,
        people: List<HomePerson> = emptyList(),
        peopleRead: Boolean = true,
        showsRecoveryCodeCard: Boolean = false,
        showsRecoveryCodePrompt: Boolean = false,
        notificationPermission: NotificationPermission? = null,
        canAskQuietly: Boolean = false,
        hasAskedQuietly: Boolean = false,
        isNotificationPromptSnoozed: Boolean = false,
    ): HomeUiState = HomeUiState(
        displayName = "Pierre",
        avatarInitial = "P",
        avatarColor = AvatarColor.GREEN,
        photoUrl = null,
        accountProvider = null,
        syncBadge = syncBadge,
        pendingChangeCount = 0,
        firstDownload = DownloadProgress(
            arrivedGroupCount = 0,
            groupCount = null,
            isWaitingForInternet = false,
        ).takeIf { isFirstDownload },
        isOnline = isOnline,
        isLoaded = isLoaded,
        groupCount = groupCount,
        people = if (peopleRead) people.toLoaded() else Loadable.Loading,
        summary = if (peopleRead) HomeSummary(totals = emptyList()).toLoaded() else Loadable.Loading,
        groups = emptyList(),
        billDays = emptyList(),
        showsRecoveryCodeCard = showsRecoveryCodeCard,
        showsRecoveryCodePrompt = showsRecoveryCodePrompt,
        recordedPayment = null,
        deletedGroupNotice = null,
        isRefreshing = false,
        activityUnreadCount = 0,
        notificationPermission = notificationPermission,
        canAskQuietly = canAskQuietly,
        hasAskedQuietly = hasAskedQuietly,
        isNotificationPromptSnoozed = isNotificationPromptSnoozed,
    )
}
