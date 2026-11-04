package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillLinkResultDto
import com.quare.blitzsplit.core.expenses.data.dto.BillShareLinkDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillAttachmentDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillLikeDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillPartDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillStatusDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillPart
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedBillMapperImplTest {
    private val mapper = SharedBillMapperImpl()

    @Test
    fun `GIVEN a link only members open WHEN mapping it THEN reads its code and MEMBERS`() {
        // Given
        val dto = BillShareLinkDto(
            code = CODE,
            visibility = "members",
        )

        // When
        val link = mapper.toDomain(dto)

        // Then
        assertEquals(BillShareLink(CODE, BillLinkVisibility.MEMBERS), link)
    }

    @Test
    fun `GIVEN a link anyone opens WHEN mapping it THEN reads its code and ANYONE`() {
        // Given
        val dto = BillShareLinkDto(
            code = CODE,
            visibility = "anyone",
        )

        // When
        val link = mapper.toDomain(dto)

        // Then
        assertEquals(BillShareLink(CODE, BillLinkVisibility.ANYONE), link)
    }

    @Test
    fun `GIVEN a link of a visibility this version does not know WHEN mapping it THEN reads the link with none`() {
        // Given
        val dto = BillShareLinkDto(
            code = CODE,
            visibility = "friends",
        )

        // When
        val link = mapper.toDomain(dto)

        // Then
        assertEquals(BillShareLink(CODE, visibility = null), link)
    }

    @Test
    fun `GIVEN the server set the link WHEN mapping its answer THEN reads the link`() {
        // Given
        val dto = BillLinkResultDto(
            status = "ok",
            code = CODE,
            visibility = "anyone",
        )

        // When
        val link = mapper.toDomain(dto)

        // Then
        assertEquals(BillShareLink(CODE, BillLinkVisibility.ANYONE), link)
    }

    @Test
    fun `GIVEN the server did not find the bill WHEN mapping its link answer THEN reads no link`() {
        // Given
        val dto = BillLinkResultDto(
            status = "not_found",
            code = null,
            visibility = null,
        )

        // When
        val link = mapper.toDomain(dto)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN a link answer of an unknown status WHEN mapping it THEN fails`() {
        // Given
        val dto = BillLinkResultDto(
            status = "forbidden",
            code = CODE,
            visibility = "anyone",
        )

        // When
        val error = assertFailsWith<IllegalStateException> { mapper.toDomain(dto) }

        // Then
        assertTrue(error.message.orEmpty().contains("forbidden"))
    }

    @Test
    fun `GIVEN an ok link answer without its code WHEN mapping it THEN fails`() {
        // Given
        val dto = BillLinkResultDto(
            status = "ok",
            code = null,
            visibility = "anyone",
        )

        // When
        val result = runCatching { mapper.toDomain(dto) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN an ok link answer of an unknown visibility WHEN mapping it THEN reads the link with none`() {
        // Given
        val dto = BillLinkResultDto(
            status = "ok",
            code = CODE,
            visibility = "friends",
        )

        // When
        val link = mapper.toDomain(dto)

        // Then
        assertEquals(BillShareLink(CODE, visibility = null), link)
    }

    @Test
    fun `GIVEN a code that is no link WHEN mapping the shared bill THEN is not found`() {
        // Given
        val dto = notFoundDto(status = "not_found")

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(SharedBill.NotFound, bill)
    }

    @Test
    fun `GIVEN a link only members open WHEN mapping the shared bill THEN is members only`() {
        // Given
        val dto = notFoundDto(status = "members_only")

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(SharedBill.MembersOnly, bill)
    }

    @Test
    fun `GIVEN a shared bill of an unknown status WHEN mapping it THEN fails`() {
        // Given
        val dto = notFoundDto(status = "expired")

        // When
        val result = runCatching { mapper.toDomain(dto, imageUrls = emptyMap()) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN an open shared bill WHEN mapping it THEN reads the bill its people its photos and comments`() {
        // Given
        val dto = sharedBillDto(
            attachments = listOf(
                attachmentDto(
                    id = "receipt-1",
                    kind = "receipt",
                    path = PATH_RECEIPT,
                ),
                attachmentDto(
                    id = "photo-1",
                    kind = "photo",
                    path = PATH_PHOTO,
                    addedByName = null,
                ),
            ),
            comments = listOf(
                commentDto(
                    id = "comment-1",
                    viaLink = true,
                ),
            ),
        )
        val imageUrls = mapOf(PATH_RECEIPT to SIGNED_RECEIPT, PATH_PHOTO to SIGNED_PHOTO)

        // When
        val bill = mapper.toDomain(dto, imageUrls)

        // Then
        assertEquals(
            SharedBill.Open(
                isMember = true,
                groupId = GROUP_ID,
                groupName = "Viagem Floripa",
                currency = GroupCurrency.BRL,
                expenseId = BILL_ID,
                description = "Jantar",
                amountMinor = 31_200L,
                spentAt = 1_000L,
                categoryId = "food",
                splitMode = SplitMode.EQUAL,
                payers = listOf(
                    SharedBillPart(
                        memberId = "member-ana",
                        name = "Ana",
                        amountMinor = 31_200L,
                    ),
                ),
                shares = listOf(
                    SharedBillPart(
                        memberId = "member-ana",
                        name = "Ana",
                        amountMinor = 15_600L,
                    ),
                    SharedBillPart(
                        memberId = "member-carla",
                        name = null,
                        amountMinor = 15_600L,
                    ),
                ),
                attachments = listOf(
                    SharedBillAttachment(
                        id = "receipt-1",
                        kind = BillAttachmentKind.RECEIPT,
                        width = 1200,
                        height = 1600,
                        addedByName = "Ana",
                        addedAt = 2_000L,
                        imageUrl = SIGNED_RECEIPT,
                    ),
                    SharedBillAttachment(
                        id = "photo-1",
                        kind = BillAttachmentKind.PHOTO,
                        width = 1200,
                        height = 1600,
                        addedByName = null,
                        addedAt = 2_000L,
                        imageUrl = SIGNED_PHOTO,
                    ),
                ),
                comments = listOf(
                    BillComment(
                        id = "comment-1",
                        expenseId = BILL_ID,
                        groupId = GROUP_ID,
                        authorUserId = null,
                        authorMemberId = null,
                        authorName = "Diego",
                        threadId = null,
                        replyToName = null,
                        body = "Quem trouxe o gelo?",
                        viaLink = true,
                        postedAt = 3_000L,
                        isPending = false,
                        editedAt = null,
                        isDeleted = false,
                        isMine = false,
                        likes = emptyList(),
                    ),
                ),
            ),
            bill,
        )
    }

    @Test
    fun `GIVEN a photo the bucket did not sign WHEN mapping the shared bill THEN reads it with no image`() {
        // Given
        val dto = sharedBillDto(
            attachments = listOf(
                attachmentDto(
                    id = "photo-1",
                    path = PATH_PHOTO,
                ),
                attachmentDto(id = "photo-2"),
            ),
        )
        val imageUrls = mapOf(PATH_PHOTO to SIGNED_PHOTO)

        // When
        val bill = mapper.toDomain(dto, imageUrls)

        // Then
        assertEquals(
            listOf("photo-1" to SIGNED_PHOTO, "photo-2" to null),
            assertIs<SharedBill.Open>(bill).attachments.map { it.id to it.imageUrl },
        )
    }

    @Test
    fun `GIVEN an attachment of a kind this version does not know WHEN mapping the shared bill THEN leaves it out`() {
        // Given
        val dto = sharedBillDto(
            attachments = listOf(
                attachmentDto(
                    id = "video-1",
                    kind = "video",
                ),
                attachmentDto(
                    id = "photo-1",
                    kind = "photo",
                ),
            ),
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(listOf("photo-1"), assertIs<SharedBill.Open>(bill).attachments.map { it.id })
    }

    @Test
    fun `GIVEN a reply to a reply WHEN mapping the shared bill THEN keeps its thread and whom it answers`() {
        // Given
        val dto = sharedBillDto(
            comments = listOf(
                commentDto(id = "comment-1"),
                commentDto(
                    id = "reply-1",
                    threadId = "comment-1",
                    replyToName = "Bruno",
                    viaLink = false,
                ),
            ),
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        val reply = assertIs<SharedBill.Open>(bill).comments.last()
        assertEquals(
            BillComment(
                id = "reply-1",
                expenseId = BILL_ID,
                groupId = GROUP_ID,
                authorUserId = null,
                authorMemberId = null,
                authorName = "Diego",
                threadId = "comment-1",
                replyToName = "Bruno",
                body = "Quem trouxe o gelo?",
                viaLink = false,
                postedAt = 3_000L,
                isPending = false,
                editedAt = null,
                isDeleted = false,
                isMine = false,
                likes = emptyList(),
            ),
            reply,
        )
    }

    @Test
    fun `GIVEN a comment with no via link flag WHEN mapping the shared bill THEN reads it not via link`() {
        // Given
        val dto = sharedBillDto(
            comments = listOf(
                commentDto(
                    id = "comment-1",
                    viaLink = null,
                ),
            ),
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(
            false,
            assertIs<SharedBill.Open>(bill)
                .comments
                .single()
                .viaLink,
        )
    }

    @Test
    fun `GIVEN my comment edited and liked WHEN mapping the shared bill THEN reads the edit mine and who liked it`() {
        // Given
        val dto = sharedBillDto(
            comments = listOf(
                commentDto(
                    id = "comment-1",
                    body = "Quem trouxe o gelo e o carvão?",
                    editedAt = 4_000L,
                    isDeleted = false,
                    isMine = true,
                    likedBy = listOf(
                        SharedBillLikeDto(
                            name = "Ana",
                            isMine = false,
                        ),
                        SharedBillLikeDto(
                            name = null,
                            isMine = true,
                        ),
                    ),
                ),
            ),
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(
            BillComment(
                id = "comment-1",
                expenseId = BILL_ID,
                groupId = GROUP_ID,
                authorUserId = null,
                authorMemberId = null,
                authorName = "Diego",
                threadId = null,
                replyToName = null,
                body = "Quem trouxe o gelo e o carvão?",
                viaLink = true,
                postedAt = 3_000L,
                editedAt = 4_000L,
                isDeleted = false,
                isMine = true,
                likes = listOf(
                    BillCommentLike(
                        memberId = null,
                        name = "Ana",
                        isMine = false,
                    ),
                    BillCommentLike(
                        memberId = null,
                        name = null,
                        isMine = true,
                    ),
                ),
                isPending = false,
            ),
            assertIs<SharedBill.Open>(bill).comments.single(),
        )
    }

    @Test
    fun `GIVEN a deleted comment WHEN mapping the shared bill THEN reads it deleted with no text`() {
        // Given
        val dto = sharedBillDto(
            comments = listOf(
                commentDto(
                    id = "comment-1",
                    body = "",
                    isDeleted = true,
                ),
            ),
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        val comment = assertIs<SharedBill.Open>(bill).comments.single()
        assertTrue(comment.isDeleted)
        assertEquals("", comment.body)
    }

    @Test
    fun `GIVEN someone else comment WHEN mapping the shared bill THEN reads it not mine`() {
        // Given
        val dto = sharedBillDto(
            comments = listOf(
                commentDto(
                    id = "comment-1",
                    isMine = false,
                ),
            ),
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertFalse(
            assertIs<SharedBill.Open>(bill)
                .comments
                .single()
                .isMine,
        )
    }

    @Test
    fun `GIVEN a split mode this version does not know WHEN mapping the shared bill THEN reads it by amounts`() {
        // Given
        val dto = sharedBillDto(bill = billDto(splitMode = "weights"))

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(SplitMode.AMOUNT, assertIs<SharedBill.Open>(bill).splitMode)
    }

    @Test
    fun `GIVEN a bill split by percentages WHEN mapping the shared bill THEN keeps the mode`() {
        // Given
        val dto = sharedBillDto(bill = billDto(splitMode = "percent"))

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals(SplitMode.PERCENT, assertIs<SharedBill.Open>(bill).splitMode)
    }

    @Test
    fun `GIVEN an answer without its lists WHEN mapping the shared bill THEN reads them empty`() {
        // Given
        val dto = sharedBillDto(
            bill = billDto(
                payers = null,
                shares = null,
            ),
            attachments = null,
            comments = null,
        )

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        val open = assertIs<SharedBill.Open>(bill)
        assertTrue(open.payers.isEmpty())
        assertTrue(open.shares.isEmpty())
        assertTrue(open.attachments.isEmpty())
        assertTrue(open.comments.isEmpty())
    }

    @Test
    fun `GIVEN an answer without who opens it WHEN mapping the shared bill THEN reads no member`() {
        // Given
        val dto = sharedBillDto(isMember = null)

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        val open = assertIs<SharedBill.Open>(bill)
        assertFalse(open.isMember)
    }

    @Test
    fun `GIVEN an answer without the group name WHEN mapping the shared bill THEN reads it empty`() {
        // Given
        val dto = sharedBillDto(groupName = null)

        // When
        val bill = mapper.toDomain(dto, imageUrls = emptyMap())

        // Then
        assertEquals("", assertIs<SharedBill.Open>(bill).groupName)
    }

    @Test
    fun `GIVEN an open answer without the bill WHEN mapping the shared bill THEN fails`() {
        // Given
        val dto = sharedBillDto(bill = null)

        // When
        val result = runCatching { mapper.toDomain(dto, imageUrls = emptyMap()) }

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN an open answer without the group WHEN mapping the shared bill THEN fails`() {
        // Given
        val dto = sharedBillDto(groupId = null)

        // When
        val result = runCatching { mapper.toDomain(dto, imageUrls = emptyMap()) }

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN an open answer of an unknown currency WHEN mapping the shared bill THEN fails`() {
        // Given
        val dto = sharedBillDto(currency = "XYZ")

        // When
        val result = runCatching { mapper.toDomain(dto, imageUrls = emptyMap()) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN the comment went up WHEN mapping its answer THEN is posted`() {
        // Given
        val dto = SharedBillStatusDto(status = "ok")

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(SharedBillComment.POSTED, outcome)
    }

    @Test
    fun `GIVEN the link stopped working WHEN mapping the comment answer THEN is not found`() {
        // Given
        val dto = SharedBillStatusDto(status = "not_found")

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(SharedBillComment.NOT_FOUND, outcome)
    }

    @Test
    fun `GIVEN the link became members only WHEN mapping the comment answer THEN is members only`() {
        // Given
        val dto = SharedBillStatusDto(status = "members_only")

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(SharedBillComment.MEMBERS_ONLY, outcome)
    }

    @Test
    fun `GIVEN the server knows no name WHEN mapping the comment answer THEN needs a name`() {
        // Given
        val dto = SharedBillStatusDto(status = "needs_name")

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(SharedBillComment.NEEDS_NAME, outcome)
    }

    @Test
    fun `GIVEN a comment answer of an unknown status WHEN mapping it THEN fails`() {
        // Given
        val dto = SharedBillStatusDto(status = "rate_limited")

        // When
        val result = runCatching { mapper.toDomain(dto) }

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun notFoundDto(status: String): SharedBillDto = SharedBillDto(
        status = status,
        isMember = null,
        canComment = null,
        groupId = null,
        groupName = null,
        currency = null,
        visibility = null,
        bill = null,
        attachments = null,
        comments = null,
    )

    private fun sharedBillDto(
        isMember: Boolean? = true,
        canComment: Boolean? = true,
        groupId: String? = GROUP_ID,
        groupName: String? = "Viagem Floripa",
        currency: String? = "BRL",
        visibility: String? = "members",
        bill: SharedBillExpenseDto? = billDto(),
        attachments: List<SharedBillAttachmentDto>? = emptyList(),
        comments: List<SharedBillCommentDto>? = emptyList(),
    ): SharedBillDto = SharedBillDto(
        status = "ok",
        isMember = isMember,
        canComment = canComment,
        groupId = groupId,
        groupName = groupName,
        currency = currency,
        visibility = visibility,
        bill = bill,
        attachments = attachments,
        comments = comments,
    )

    private fun billDto(
        splitMode: String = "equal",
        payers: List<SharedBillPartDto>? = listOf(SharedBillPartDto("member-ana", "Ana", 31_200L)),
        shares: List<SharedBillPartDto>? = listOf(
            SharedBillPartDto("member-ana", "Ana", 15_600L),
            SharedBillPartDto("member-carla", null, 15_600L),
        ),
    ): SharedBillExpenseDto = SharedBillExpenseDto(
        id = BILL_ID,
        description = "Jantar",
        amountMinor = 31_200L,
        spentAt = 1_000L,
        categoryId = "food",
        splitMode = splitMode,
        payers = payers,
        shares = shares,
    )

    private fun attachmentDto(
        id: String,
        kind: String = "photo",
        path: String = "$GROUP_ID/$BILL_ID/$id.jpg",
        addedByName: String? = "Ana",
    ): SharedBillAttachmentDto = SharedBillAttachmentDto(
        id = id,
        kind = kind,
        path = path,
        width = 1200,
        height = 1600,
        addedByName = addedByName,
        addedAt = 2_000L,
    )

    private fun commentDto(
        id: String,
        threadId: String? = null,
        replyToName: String? = null,
        viaLink: Boolean? = true,
        body: String = "Quem trouxe o gelo?",
        editedAt: Long? = null,
        isDeleted: Boolean? = null,
        isMine: Boolean? = null,
        likedBy: List<SharedBillLikeDto>? = null,
    ): SharedBillCommentDto = SharedBillCommentDto(
        id = id,
        threadId = threadId,
        authorName = "Diego",
        replyToName = replyToName,
        body = body,
        viaLink = viaLink,
        postedAt = 3_000L,
        editedAt = editedAt,
        isDeleted = isDeleted,
        isMine = isMine,
        likedBy = likedBy,
    )

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val GROUP_ID = "group-1"
        const val BILL_ID = "bill-1"
        const val PATH_RECEIPT = "group-1/bill-1/receipt-1.jpg"
        const val PATH_PHOTO = "group-1/bill-1/photo-1.jpg"
        const val SIGNED_RECEIPT = "https://signed/receipt-1"
        const val SIGNED_PHOTO = "https://signed/photo-1"
    }
}
