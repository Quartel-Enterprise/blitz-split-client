package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillPart
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SharedBillRowsTest {
    @Test
    fun `GIVEN Ana and Bruno paid WHEN listing the payers THEN names both in order with their initials`() {
        // Given
        val bill = bill(payers = listOf(part(ANA, "Ana Paula", 30_000), part(BRUNO, "Bruno", 34_200)))

        // When
        val payers = payersOf(bill)

        // Then
        assertEquals(
            listOf(
                BillPayer(
                    name = "Ana Paula",
                    initials = "AP",
                    avatarColor = AvatarColor.default,
                    photoUrl = null,
                    isMe = false,
                ),
                BillPayer(
                    name = "Bruno",
                    initials = "BR",
                    avatarColor = AvatarColor.default,
                    photoUrl = null,
                    isMe = false,
                ),
            ),
            payers,
        )
    }

    @Test
    fun `GIVEN whoever paid left the group WHEN listing the payers THEN the payer has no name nor initials`() {
        // Given
        val bill = bill(payers = listOf(part(FORMER, null, 64_200)))

        // When
        val payer = payersOf(bill).single()

        // Then
        assertNull(payer.name)
        assertEquals("", payer.initials)
    }

    @Test
    fun `GIVEN Bruno paid and three split it WHEN listing the people THEN shows each part and what Bruno paid`() {
        // Given
        val bill = bill()

        // When
        val people = peopleOf(bill)

        // Then
        assertEquals(listOf(ANA, BRUNO, CARLA), people.map { it.memberId })
        assertEquals(listOf("Ana Paula", "Bruno", "Carla"), people.map { it.name })
        assertEquals(listOf(null, 64_200L, null), people.map { it.paidMinor })
        assertEquals(listOf(21_400L, 21_400L, 21_400L), people.map { it.shareMinor })
    }

    @Test
    fun `GIVEN Diego paid without a part WHEN listing the people THEN he comes last with what he paid and no part`() {
        // Given
        val bill = bill(payers = listOf(part(DIEGO, "Diego", 64_200)))

        // When
        val people = peopleOf(bill)

        // Then
        assertEquals(listOf(ANA, BRUNO, CARLA, DIEGO), people.map { it.memberId })
        assertEquals(
            BillShareRow(
                memberId = DIEGO,
                name = "Diego",
                initials = "DI",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                isMe = false,
                hasJoined = true,
                paidMinor = 64_200,
                shareMinor = null,
            ),
            people.last(),
        )
    }

    @Test
    fun `GIVEN someone who left the group has a part WHEN listing the people THEN that row has no name`() {
        // Given
        val bill = bill(shares = listOf(part(ANA, "Ana Paula", 32_100), part(FORMER, null, 32_100)))

        // When
        val row = peopleOf(bill).first { it.memberId == FORMER }

        // Then
        assertNull(row.name)
        assertEquals("", row.initials)
        assertEquals(32_100L, row.shareMinor)
    }

    @Test
    fun `GIVEN anyone in the bill WHEN listing the people THEN nobody is the device user nor waiting to join`() {
        // Given
        val bill = bill()

        // When
        val people = peopleOf(bill)

        // Then
        assertTrue(people.none { it.isMe })
        assertTrue(people.all { it.hasJoined })
        assertTrue(people.all { it.photoUrl == null })
    }

    @Test
    fun `GIVEN the receipt and two photos WHEN making the tiles THEN only the photos are numbered 1 and 2`() {
        // Given
        val attachments = listOf(
            attachment("receipt", BillAttachmentKind.RECEIPT),
            attachment("photo-1"),
            attachment("photo-2"),
        )

        // When
        val tiles = sharedTilesOf(attachments, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("receipt", "photo-1", "photo-2"), tiles.map { it.id })
        assertEquals(listOf(true, false, false), tiles.map { it.isReceipt })
        assertEquals(listOf(null, 1, 2), tiles.map { it.photoNumber })
    }

    @Test
    fun `GIVEN the receipt and a photo WHEN making the tiles of a link THEN neither can be removed`() {
        // Given
        val attachments = listOf(
            attachment("receipt", BillAttachmentKind.RECEIPT),
            attachment("photo-1"),
        )

        // When
        val tiles = sharedTilesOf(attachments, now, TimeZone.UTC)

        // Then
        assertEquals(listOf(false, false), tiles.map { it.canRemove })
    }

    @Test
    fun `GIVEN a photo with its signed URL WHEN making the tiles THEN keeps its URL size adder and time`() {
        // Given
        val attachments = listOf(attachment("photo-1"))

        // When
        val tile = sharedTilesOf(attachments, now, TimeZone.UTC).single()

        // Then
        assertEquals("https://signed/photo-1.jpg", tile.imageUrl)
        assertNull(tile.localImage)
        assertEquals(1600, tile.width)
        assertEquals(1200, tile.height)
        assertEquals("Ana", tile.adderName)
        assertEquals(now - HOUR, tile.addedAt)
        assertFalse(tile.isAddedByMe)
        assertFalse(tile.isPending)
    }

    @Test
    fun `GIVEN a photo the bucket could not sign WHEN making the tiles THEN has no image to draw`() {
        // Given
        val attachments = listOf(attachment("photo-1", imageUrl = null))

        // When
        val tile = sharedTilesOf(attachments, now, TimeZone.UTC).single()

        // Then
        assertNull(tile.imageUrl)
    }

    @Test
    fun `GIVEN a photo added by someone who left the group WHEN making the tiles THEN has no name`() {
        // Given
        val attachments = listOf(attachment("photo-1", addedByName = null))

        // When
        val tile = sharedTilesOf(attachments, now, TimeZone.UTC).single()

        // Then
        assertNull(tile.adderName)
    }

    @Test
    fun `GIVEN photos of today yesterday and last week WHEN making the tiles THEN says the day of each`() {
        // Given
        val attachments = listOf(
            attachment("today", addedAt = now - HOUR),
            attachment("yesterday", addedAt = now - DAY),
            attachment("last-week", addedAt = now - 7 * DAY),
        )

        // When
        val tiles = sharedTilesOf(attachments, now, TimeZone.UTC)

        // Then
        assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER), tiles.map { it.addedDay })
    }

    @Test
    fun `GIVEN a photo of early today in UTC WHEN making the tiles three hours behind THEN it was added yesterday`() {
        // Given
        val earlyToday = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()
        val attachments = listOf(attachment("photo-1", addedAt = earlyToday))

        // When
        val tile = sharedTilesOf(attachments, now, UtcOffset(hours = -3).asTimeZone()).single()

        // Then
        assertEquals(RelativeDay.YESTERDAY, tile.addedDay)
    }

    private val now = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()

    private fun part(
        memberId: String,
        name: String?,
        amountMinor: Long,
    ): SharedBillPart = SharedBillPart(
        memberId = memberId,
        name = name,
        amountMinor = amountMinor,
    )

    // Why: by default Bruno paid R$ 642 split equally among Ana, Bruno and Carla.
    private fun bill(
        payers: List<SharedBillPart> = listOf(part(BRUNO, "Bruno", 64_200)),
        shares: List<SharedBillPart> = listOf(
            part(ANA, "Ana Paula", 21_400),
            part(BRUNO, "Bruno", 21_400),
            part(CARLA, "Carla", 21_400),
        ),
    ): SharedBill.Open = SharedBill.Open(
        isMember = false,
        groupId = "group-1",
        groupName = "Viagem Floripa",
        currency = GroupCurrency.BRL,
        expenseId = "expense-1",
        description = "Jantar no Ostradamus",
        amountMinor = 64_200,
        spentAt = now,
        categoryId = null,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = shares,
        attachments = emptyList(),
        comments = emptyList(),
    )

    private fun attachment(
        id: String,
        kind: BillAttachmentKind = BillAttachmentKind.PHOTO,
        addedByName: String? = "Ana",
        addedAt: Long = now - HOUR,
        imageUrl: String? = "https://signed/$id.jpg",
    ): SharedBillAttachment = SharedBillAttachment(
        id = id,
        kind = kind,
        width = 1600,
        height = 1200,
        addedByName = addedByName,
        addedAt = addedAt,
        imageUrl = imageUrl,
    )

    private companion object {
        const val ANA = "member-1"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val DIEGO = "member-5"
        const val FORMER = "member-9"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
