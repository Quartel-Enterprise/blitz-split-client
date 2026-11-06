package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BillAttachmentsTest {
    @Test
    fun `GIVEN the receipt and two photos WHEN making the tiles THEN only the photos are numbered 1 and 2`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "receipt",
                kind = BillAttachmentKind.RECEIPT,
            ),
            attachment(id = "photo-a"),
            attachment(id = "photo-b"),
        )

        // When
        val tiles = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("receipt", "photo-a", "photo-b"), tiles.map { it.id })
        assertEquals(listOf(true, false, false), tiles.map { it.isReceipt })
        assertEquals(listOf(null, 1, 2), tiles.map { it.photoNumber })
    }

    @Test
    fun `GIVEN two photos and no receipt WHEN making the tiles THEN numbers them 1 and 2 in their order`() {
        // Given
        val attachments = listOf(
            attachment(id = "photo-a"),
            attachment(id = "photo-b"),
        )

        // When
        val tiles = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("photo-a", "photo-b"), tiles.map { it.id })
        assertEquals(listOf(1, 2), tiles.map { it.photoNumber })
    }

    @Test
    fun `GIVEN the receipt and a photo WHEN making the tiles THEN only the photo can be removed`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "receipt",
                kind = BillAttachmentKind.RECEIPT,
            ),
            attachment(id = "photo-a"),
        )

        // When
        val tiles = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC)

        // Then
        assertFalse(tiles[0].canRemove)
        assertTrue(tiles[1].canRemove)
    }

    @Test
    fun `GIVEN a photo added here not up yet WHEN making the tiles THEN draws its bytes and says it is pending`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "photo-a",
                isPending = true,
                hasPendingImage = true,
            ),
        )
        val jpeg = byteArrayOf(1, 2, 3)

        // When
        val tile = tilesOf(attachments, emptyMap(), mapOf("photo-a" to jpeg), group, now, TimeZone.UTC).single()

        // Then
        assertEquals(jpeg, tile.localImage)
        assertNull(tile.imageUrl)
        assertTrue(tile.isPending)
    }

    @Test
    fun `GIVEN a synced photo with its signed URL WHEN making the tiles THEN draws the URL and it is not pending`() {
        // Given
        val attachments = listOf(attachment(id = "photo-a"))
        val imageUrls = mapOf("photo-a" to "https://bucket/photo-a.jpg")

        // When
        val tile = tilesOf(attachments, imageUrls, emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertEquals("https://bucket/photo-a.jpg", tile.imageUrl)
        assertFalse(tile.isPending)
    }

    @Test
    fun `GIVEN a synced photo without a signed URL yet WHEN making the tiles THEN has no image to draw`() {
        // Given
        val attachments = listOf(attachment(id = "photo-a"))
        val imageUrls = mapOf("photo-b" to "https://bucket/photo-b.jpg")

        // When
        val tile = tilesOf(attachments, imageUrls, emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertNull(tile.imageUrl)
    }

    @Test
    fun `GIVEN online a synced photo without a signed URL yet WHEN making the tiles THEN its image is loading`() {
        // Given
        val attachments = listOf(attachment(id = "photo-a"))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC, isOnline = true).single()

        // Then
        assertTrue(tile.isImageLoading)
    }

    @Test
    fun `GIVEN online the receipt without a signed URL yet WHEN making the tiles THEN its image is loading`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "receipt",
                kind = BillAttachmentKind.RECEIPT,
            ),
        )

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC, isOnline = true).single()

        // Then
        assertTrue(tile.isImageLoading)
    }

    @Test
    fun `GIVEN offline a synced photo without a signed URL WHEN making the tiles THEN its image is not loading`() {
        // Given
        val attachments = listOf(attachment(id = "photo-a"))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC, isOnline = false).single()

        // Then
        assertFalse(tile.isImageLoading)
    }

    @Test
    fun `GIVEN online a synced photo with its signed URL WHEN making the tiles THEN its image is not loading`() {
        // Given
        val attachments = listOf(attachment(id = "photo-a"))
        val imageUrls = mapOf("photo-a" to "https://bucket/photo-a.jpg")

        // When
        val tile = tilesOf(attachments, imageUrls, emptyMap(), group, now, TimeZone.UTC, isOnline = true).single()

        // Then
        assertFalse(tile.isImageLoading)
    }

    @Test
    fun `GIVEN online only another photo has its signed URL WHEN making the tiles THEN this one is loading`() {
        // Given
        val attachments = listOf(
            attachment(id = "photo-a"),
            attachment(id = "photo-b"),
        )
        val imageUrls = mapOf("photo-b" to "https://bucket/photo-b.jpg")

        // When
        val tiles = tilesOf(attachments, imageUrls, emptyMap(), group, now, TimeZone.UTC, isOnline = true)

        // Then
        assertEquals(listOf(true, false), tiles.map { it.isImageLoading })
    }

    @Test
    fun `GIVEN online a photo added here not up yet WHEN making the tiles THEN its image is not loading`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "photo-a",
                isPending = true,
                hasPendingImage = true,
            ),
        )
        val localImages = mapOf("photo-a" to byteArrayOf(1, 2, 3))

        // When
        val tile = tilesOf(attachments, emptyMap(), localImages, group, now, TimeZone.UTC, isOnline = true).single()

        // Then
        assertFalse(tile.isImageLoading)
    }

    @Test
    fun `GIVEN online a photo added here already up WHEN making the tiles THEN its bytes draw and it is not loading`() {
        // Given
        val attachments = listOf(attachment(id = "photo-a"))
        val localImages = mapOf("photo-a" to byteArrayOf(1, 2, 3))

        // When
        val tile = tilesOf(attachments, emptyMap(), localImages, group, now, TimeZone.UTC, isOnline = true).single()

        // Then
        assertFalse(tile.isImageLoading)
    }

    @Test
    fun `GIVEN online a photo to go up whose bytes were not read WHEN making the tiles THEN it is not loading`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "photo-a",
                isPending = true,
                hasPendingImage = true,
            ),
        )

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC, isOnline = true).single()

        // Then
        assertFalse(tile.isImageLoading)
    }

    @Test
    fun `GIVEN a photo with its size and time WHEN making the tiles THEN keeps them`() {
        // Given
        val addedAt = now - HOUR
        val attachments = listOf(
            attachment(
                id = "photo-a",
                width = 1200,
                height = 1600,
                addedAt = addedAt,
            ),
        )

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertEquals(1200, tile.width)
        assertEquals(1600, tile.height)
        assertEquals(addedAt, tile.addedAt)
    }

    @Test
    fun `GIVEN a photo added by Ana WHEN making the tiles THEN names Ana and it is not by the device user`() {
        // Given
        val attachments = listOf(attachment(addedByMemberId = ANA))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertEquals("Ana", tile.adderName)
        assertFalse(tile.isAddedByMe)
    }

    @Test
    fun `GIVEN a photo added by the device user WHEN making the tiles THEN it is added by them`() {
        // Given
        val attachments = listOf(attachment(addedByMemberId = PIERRE))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertEquals("Pierre", tile.adderName)
        assertTrue(tile.isAddedByMe)
    }

    @Test
    fun `GIVEN Ana was renamed after adding a photo WHEN making the tiles THEN names her as she is now`() {
        // Given
        val attachments = listOf(attachment(addedByMemberId = ANA))
        val renamed = group.copy(members = listOf(ana.copy(name = "Ana Paula"), pierre))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), renamed, now, TimeZone.UTC).single()

        // Then
        assertEquals("Ana Paula", tile.adderName)
    }

    @Test
    fun `GIVEN a photo added by someone who left the group WHEN making the tiles THEN has no name`() {
        // Given
        val attachments = listOf(attachment(addedByMemberId = FORMER))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertNull(tile.adderName)
        assertFalse(tile.isAddedByMe)
    }

    @Test
    fun `GIVEN a photo with nobody as its adder WHEN making the tiles THEN has no name`() {
        // Given
        val attachments = listOf(attachment(addedByMemberId = null))

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC).single()

        // Then
        assertNull(tile.adderName)
        assertFalse(tile.isAddedByMe)
    }

    @Test
    fun `GIVEN photos of today yesterday and last week WHEN making the tiles THEN says the day of each`() {
        // Given
        val attachments = listOf(
            attachment(
                id = "today",
                addedAt = now - HOUR,
            ),
            attachment(
                id = "yesterday",
                addedAt = now - DAY,
            ),
            attachment(
                id = "last-week",
                addedAt = now - 7 * DAY,
            ),
        )

        // When
        val tiles = tilesOf(attachments, emptyMap(), emptyMap(), group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER), tiles.map { it.addedDay })
    }

    @Test
    fun `GIVEN a photo of early today in UTC WHEN making the tiles three hours behind THEN it was added yesterday`() {
        // Given
        val attachments = listOf(attachment(addedAt = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()))
        val brasilia = UtcOffset(hours = -3).asTimeZone()

        // When
        val tile = tilesOf(attachments, emptyMap(), emptyMap(), group, now, brasilia).single()

        // Then
        assertEquals(RelativeDay.YESTERDAY, tile.addedDay)
    }

    private val now = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    private val ana = member(ANA, "Ana")
    private val pierre = member(PIERRE, "Pierre", isMe = true)

    private val group = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun attachment(
        id: String = "photo-a",
        kind: BillAttachmentKind = BillAttachmentKind.PHOTO,
        addedByMemberId: String? = ANA,
        addedAt: Long = now - HOUR,
        width: Int? = 1600,
        height: Int? = 1200,
        isPending: Boolean = false,
        hasPendingImage: Boolean = false,
    ): BillAttachment = BillAttachment(
        id = id,
        expenseId = EXPENSE_ID,
        groupId = GROUP_ID,
        kind = kind,
        width = width,
        height = height,
        addedByUserId = null,
        addedByMemberId = addedByMemberId,
        addedAt = addedAt,
        updatedAt = addedAt,
        isPending = isPending,
        hasPendingImage = hasPendingImage,
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val FORMER = "member-9"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
