package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActivityUiStateTest {
    @Test
    fun `GIVEN items on two days WHEN reading the items THEN lists them in the order of the days`() {
        // Given
        val state = state(
            days = listOf(
                day(item("a"), item("b")),
                day(item("c")),
            ),
        )

        // When
        val ids = state.items.map { it.id }

        // Then
        assertEquals(listOf("a", "b", "c"), ids)
    }

    @Test
    fun `GIVEN an item picked still shown WHEN reading the selected item THEN is the one picked`() {
        // Given
        val state = state(
            days = listOf(day(item("a"), item("b"))),
            pickedItemId = "b",
        )

        // When
        val selected = state.selectedItem

        // Then
        assertEquals("b", selected?.id)
    }

    @Test
    fun `GIVEN an item picked the filter hides WHEN reading the selected item THEN is the first shown`() {
        // Given
        val state = state(
            days = listOf(day(item("a"), item("b"))),
            pickedItemId = "hidden",
        )

        // When
        val selected = state.selectedItem

        // Then
        assertEquals("a", selected?.id)
    }

    @Test
    fun `GIVEN no items WHEN reading the selected item THEN there is none`() {
        // Given
        val state = state(
            days = emptyList(),
            pickedItemId = "a",
        )

        // When
        val selected = state.selectedItem

        // Then
        assertNull(selected)
    }

    @Test
    fun `GIVEN a bill picked WHEN reading the related items THEN lists the other events of that bill`() {
        // Given
        val state = state(
            days = listOf(
                day(
                    item(
                        "edited",
                        kind = ActivityKind.BILL_EDITED,
                        targetId = "jantar",
                    ),
                    item("other-bill", targetId = "mercado"),
                ),
                day(item("created", targetId = "jantar")),
            ),
            pickedItemId = "created",
        )

        // When
        val related = state.relatedItems

        // Then
        assertEquals(listOf("edited"), related.map { it.id })
    }

    @Test
    fun `GIVEN something other than a bill picked WHEN reading the related items THEN there are none`() {
        // Given
        val state = state(
            days = listOf(
                day(
                    item(
                        "payment",
                        kind = ActivityKind.PAYMENT_RECORDED,
                        targetId = "settlement",
                    ),
                    item(
                        "payment-undone",
                        kind = ActivityKind.PAYMENT_UNDONE,
                        targetId = "settlement",
                    ),
                ),
            ),
            pickedItemId = "payment",
        )

        // When
        val related = state.relatedItems

        // Then
        assertTrue(related.isEmpty())
    }

    @Test
    fun `GIVEN a bill picked with no target WHEN reading the related items THEN there are none`() {
        // Given
        val state = state(
            days = listOf(day(item("created", targetId = null), item("other", targetId = null))),
            pickedItemId = "created",
        )

        // When
        val related = state.relatedItems

        // Then
        assertTrue(related.isEmpty())
    }

    @Test
    fun `GIVEN a new device syncing with nothing WHEN reading the state THEN shows placeholders`() {
        // Given
        val state = state(
            days = emptyList(),
            hasDownloaded = false,
            isSyncing = true,
        )

        // When
        val status = state.status

        // Then
        assertEquals(ActivityStatus.SYNCING, status)
        assertTrue(state.showsPlaceholders)
        assertFalse(state.showsNotSyncedNotice)
        assertFalse(state.showsTools)
        assertNull(state.emptyState)
    }

    @Test
    fun `GIVEN a new device syncing with local changes WHEN reading the state THEN shows the notice`() {
        // Given
        val state = state(
            days = listOf(day(item("queued"))),
            hasDownloaded = false,
            isSyncing = true,
        )

        // When
        val showsPlaceholders = state.showsPlaceholders

        // Then
        assertFalse(showsPlaceholders)
        assertTrue(state.showsNotSyncedNotice)
        assertNull(state.status)
    }

    @Test
    fun `GIVEN a device never synced not syncing WHEN reading the state THEN shows the notice`() {
        // Given
        val state = state(
            days = emptyList(),
            hasDownloaded = false,
            isSyncing = false,
        )

        // When
        val showsNotSyncedNotice = state.showsNotSyncedNotice

        // Then
        assertTrue(showsNotSyncedNotice)
        assertFalse(state.showsPlaceholders)
        assertEquals(ActivityEmptyState.NOTHING_YET, state.emptyState)
    }

    @Test
    fun `GIVEN the device not read yet WHEN reading the state THEN shows nothing at all`() {
        // Given
        val state = state(
            days = emptyList(),
            hasDownloaded = false,
            isSyncing = true,
            isLoaded = false,
        )

        // When
        val emptyState = state.emptyState

        // Then
        assertNull(emptyState)
        assertFalse(state.showsPlaceholders)
        assertFalse(state.showsNotSyncedNotice)
        assertFalse(state.showsTools)
    }

    @Test
    fun `GIVEN offline and a failed sync WHEN reading the status THEN says offline`() {
        // Given
        val state = state(
            isOnline = false,
            hasSyncFailed = true,
        )

        // When
        val status = state.status

        // Then
        assertEquals(ActivityStatus.OFFLINE, status)
    }

    @Test
    fun `GIVEN online and a failed sync WHEN reading the status THEN says the server did not answer`() {
        // Given
        val state = state(hasSyncFailed = true)

        // When
        val status = state.status

        // Then
        assertEquals(ActivityStatus.SERVER_ERROR, status)
    }

    @Test
    fun `GIVEN online and synced WHEN reading the status THEN there is none`() {
        // Given
        val state = state()

        // When
        val status = state.status

        // Then
        assertNull(status)
    }

    @Test
    fun `GIVEN nothing happened and no filter WHEN reading the state THEN says nothing here yet without the tools`() {
        // Given
        val state = state(days = emptyList())

        // When
        val emptyState = state.emptyState

        // Then
        assertEquals(ActivityEmptyState.NOTHING_YET, emptyState)
        assertFalse(state.showsTools)
    }

    @Test
    fun `GIVEN a filter nothing matches WHEN reading the state THEN says nothing with these filters with the tools`() {
        // Given
        val state = state(
            days = emptyList(),
            filter = ActivityFilter.none.copy(types = listOf(ActivityType.CHARGES)),
        )

        // When
        val emptyState = state.emptyState

        // Then
        assertEquals(ActivityEmptyState.NO_RESULTS, emptyState)
        assertTrue(state.showsTools)
    }

    @Test
    fun `GIVEN items WHEN reading the state THEN shows the tools and no empty state`() {
        // Given
        val state = state()

        // When
        val showsTools = state.showsTools

        // Then
        assertTrue(showsTools)
        assertNull(state.emptyState)
    }

    @Test
    fun `GIVEN unread events WHEN reading the state THEN marking all read is enabled`() {
        // Given
        val state = state(unreadCount = 2)

        // When
        val canMarkAllRead = state.canMarkAllRead

        // Then
        assertTrue(canMarkAllRead)
    }

    @Test
    fun `GIVEN nothing unread WHEN reading the state THEN marking all read is disabled`() {
        // Given
        val state = state(unreadCount = 0)

        // When
        val canMarkAllRead = state.canMarkAllRead

        // Then
        assertFalse(canMarkAllRead)
    }

    @Test
    fun `GIVEN offline with a downloaded feed WHEN reading the state THEN shows the end of the offline feed`() {
        // Given
        val state = state(isOnline = false)

        // When
        val showsOfflineEnd = state.showsOfflineEnd

        // Then
        assertTrue(showsOfflineEnd)
    }

    @Test
    fun `GIVEN offline never downloaded WHEN reading the state THEN does not show the offline end`() {
        // Given
        val state = state(
            isOnline = false,
            hasDownloaded = false,
        )

        // When
        val showsOfflineEnd = state.showsOfflineEnd

        // Then
        assertFalse(showsOfflineEnd)
    }

    @Test
    fun `GIVEN online WHEN reading the state THEN does not show the end of the offline feed`() {
        // Given
        val state = state()

        // When
        val showsOfflineEnd = state.showsOfflineEnd

        // Then
        assertFalse(showsOfflineEnd)
    }

    @Test
    fun `GIVEN a group filter and two types WHEN reading the chips THEN names the group and lists the types`() {
        // Given
        val state = state(
            filter = ActivityFilter(
                groupId = "ape",
                types = listOf(ActivityType.BILLS, ActivityType.CHARGES),
                personKey = null,
            ),
        )

        // When
        val groupChipName = state.groupChipName

        // Then
        assertEquals("Apê 302", groupChipName)
        assertEquals(listOf(ActivityType.BILLS, ActivityType.CHARGES), state.typeChips)
    }

    @Test
    fun `GIVEN a filter on a group gone WHEN reading the chips THEN the group chip has no name`() {
        // Given
        val state = state(filter = ActivityFilter.none.copy(groupId = "praia"))

        // When
        val groupChipName = state.groupChipName

        // Then
        assertNull(groupChipName)
    }

    @Test
    fun `GIVEN a draft WHEN reading the state THEN the filter is open`() {
        // Given
        val state = state(filterDraft = ActivityFilter.none)

        // When
        val isFilterOpen = state.isFilterOpen

        // Then
        assertTrue(isFilterOpen)
    }

    @Test
    fun `GIVEN only a person filter WHEN reading the filter THEN it is active`() {
        // Given
        val filter = ActivityFilter.none.copy(personKey = "user:ana")

        // When
        val isActive = filter.isActive

        // Then
        assertTrue(isActive)
        assertFalse(ActivityFilter.none.isActive)
    }

    @Test
    fun `GIVEN a bill edit with three changes WHEN reading the item THEN shows the first and two more`() {
        // Given
        val item = item(
            "edit",
            kind = ActivityKind.BILL_EDITED,
            changes = 3,
        )

        // When
        val moreChangesCount = item.moreChangesCount

        // Then
        assertEquals(2, moreChangesCount)
        assertTrue(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN a bill edit with no changes WHEN reading the item THEN has nothing more nor changes to show`() {
        // Given
        val item = item(
            "edit",
            kind = ActivityKind.BILL_EDITED,
            changes = 0,
        )

        // When
        val moreChangesCount = item.moreChangesCount

        // Then
        assertEquals(0, moreChangesCount)
        assertFalse(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN a group edit with changes WHEN reading the item THEN has no changes to show`() {
        // Given
        val item = item(
            "edit",
            kind = ActivityKind.GROUP_EDITED,
            changes = 2,
        )

        // When
        val hasChangesToShow = item.hasChangesToShow

        // Then
        assertFalse(hasChangesToShow)
    }

    @Test
    fun `GIVEN an item whose bill was deleted WHEN reading the item THEN its target is deleted`() {
        // Given
        val item = item("created").copy(
            deletion = ActivityDeletion(
                isGroup = false,
                subject = "Pastel",
                byName = "Bruno",
                byMe = false,
                atMillis = 0L,
            ),
        )

        // When
        val isTargetDeleted = item.isTargetDeleted

        // Then
        assertTrue(isTargetDeleted)
        assertFalse(item("live").isTargetDeleted)
    }

    private fun day(vararg items: ActivityItem): ActivityDay = ActivityDay(
        dayStartMillis = 0L,
        relativeDay = RelativeDay.TODAY,
        items = items.toList(),
    )

    private fun item(
        id: String,
        kind: ActivityKind = ActivityKind.BILL_CREATED,
        targetId: String? = "mercado",
        changes: Int = 0,
    ): ActivityItem = ActivityItem(
        id = id,
        kind = kind,
        actor = ActivityActor(
            name = "Ana",
            isMe = false,
            initials = "AN",
            color = AvatarColor.GREEN,
        ),
        groupId = "ape",
        groupName = "Apê 302",
        groupColor = AvatarColor.PEACH,
        groupType = GroupType.HOME,
        occurredAt = 0L,
        subject = "Mercado",
        amountMinor = 10_000L,
        currency = null,
        code = null,
        fromName = null,
        fromIsMe = false,
        toName = null,
        toIsMe = false,
        changes = List(changes) {
            val value = ActivityChangeValue(
                minor = it.toLong(),
                currency = null,
                text = null,
                millis = null,
                code = null,
                names = emptyList(),
                hasFormerMember = false,
            )
            ActivityChangeRow(ActivityField.AMOUNT, value, value)
        },
        isPending = false,
        isUnread = false,
        target = ActivityTarget.BILL,
        deletion = null,
        targetId = targetId,
    )

    private fun state(
        days: List<ActivityDay> = listOf(day(item("a"))),
        filter: ActivityFilter = ActivityFilter.none,
        filterDraft: ActivityFilter? = null,
        pickedItemId: String? = null,
        unreadCount: Int = 0,
        isOnline: Boolean = true,
        hasSyncFailed: Boolean = false,
        isSyncing: Boolean = false,
        hasDownloaded: Boolean = true,
        isLoaded: Boolean = true,
    ): ActivityUiState = ActivityUiState(
        displayName = "Pierre",
        avatarInitial = "P",
        avatarColor = AvatarColor.BLUE,
        photoUrl = null,
        accountProvider = null,
        days = days,
        unreadCount = unreadCount,
        filter = filter,
        filterDraft = filterDraft,
        draftResultCount = 0,
        groups = listOf(ActivityGroupOption("ape", "Apê 302", AvatarColor.PEACH, GroupType.HOME)),
        personName = null,
        expandedItemId = null,
        pickedItemId = pickedItemId,
        queuedCount = 0,
        isOnline = isOnline,
        hasSyncFailed = hasSyncFailed,
        isSyncing = isSyncing,
        hasDownloaded = hasDownloaded,
        isLoaded = isLoaded,
        isLoadingMore = false,
        isRefreshing = false,
        hasMore = false,
        deletedNotice = null,
    )
}
