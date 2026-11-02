package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.BillAttachmentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillShareLinkRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.data.dto.BillLinkResultDto
import com.quare.blitzsplit.core.expenses.data.dto.BillShareLinkDto
import com.quare.blitzsplit.core.expenses.data.dto.CommentOnSharedBillParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillAttachmentDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillStatusDto
import com.quare.blitzsplit.core.expenses.data.mapper.SharedBillMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.BillShareLinkRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class BillShareLinkRepositoryImplTest {
    private lateinit var remote: FakeBillShareLinkRemoteDataSource
    private lateinit var attachmentRemote: FakeSigningRemoteDataSource
    private lateinit var repository: BillShareLinkRepositoryImpl

    @Test
    fun `GIVEN a bill with a link WHEN loading it THEN reads the link`() = runTest {
        // Given
        prepareScenario(
            storedLink = BillShareLinkDto(
                code = CODE,
                visibility = "members",
            ),
        )

        // When
        val result = repository.fetchLink(BILL_ID)

        // Then
        assertEquals(BillShareLink(CODE, BillLinkVisibility.MEMBERS), result.getOrThrow())
        assertEquals(listOf(BILL_ID), remote.fetchedLinkIds)
    }

    @Test
    fun `GIVEN a bill without a link WHEN loading it THEN reads no link`() = runTest {
        // Given
        prepareScenario(storedLink = null)

        // When
        val result = repository.fetchLink(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN no internet WHEN loading the link THEN fails`() = runTest {
        // Given
        prepareScenario(remoteError = IllegalStateException("offline"))

        // When
        val result = repository.fetchLink(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a bill WHEN letting anyone open its link THEN sends anyone and reads the link`() = runTest {
        // Given
        prepareScenario(
            updateAnswer = BillLinkResultDto(
                status = "ok",
                code = CODE,
                visibility = "anyone",
            ),
        )

        // When
        val result = repository.updateLink(BILL_ID, BillLinkVisibility.ANYONE)

        // Then
        assertEquals(BillShareLink(CODE, BillLinkVisibility.ANYONE), result.getOrThrow())
        assertEquals(listOf(BILL_ID to "anyone"), remote.updates)
    }

    @Test
    fun `GIVEN a bill WHEN letting only members open its link THEN sends members`() = runTest {
        // Given
        prepareScenario(
            updateAnswer = BillLinkResultDto(
                status = "ok",
                code = CODE,
                visibility = "members",
            ),
        )

        // When
        val result = repository.updateLink(BILL_ID, BillLinkVisibility.MEMBERS)

        // Then
        assertEquals(BillShareLink(CODE, BillLinkVisibility.MEMBERS), result.getOrThrow())
        assertEquals(listOf(BILL_ID to "members"), remote.updates)
    }

    @Test
    fun `GIVEN a bill gone from the server WHEN setting its link THEN reads no link`() = runTest {
        // Given
        prepareScenario(
            updateAnswer = BillLinkResultDto(
                status = "not_found",
                code = null,
                visibility = null,
            ),
        )

        // When
        val result = repository.updateLink(BILL_ID, BillLinkVisibility.ANYONE)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN an answer of an unknown status WHEN setting the link THEN fails`() = runTest {
        // Given
        prepareScenario(
            updateAnswer = BillLinkResultDto(
                status = "forbidden",
                code = null,
                visibility = null,
            ),
        )

        // When
        val result = repository.updateLink(BILL_ID, BillLinkVisibility.ANYONE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN no internet WHEN setting the link THEN fails`() = runTest {
        // Given
        prepareScenario(remoteError = IllegalStateException("offline"))

        // When
        val result = repository.updateLink(BILL_ID, BillLinkVisibility.ANYONE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a bill with a link WHEN disabling it THEN asks the server to disable that bill link`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.disableLink(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(BILL_ID), remote.disabledIds)
    }

    @Test
    fun `GIVEN a bill gone from the server WHEN disabling its link THEN succeeds`() = runTest {
        // Given
        prepareScenario(
            disableAnswer = BillLinkResultDto(
                status = "not_found",
                code = null,
                visibility = null,
            ),
        )

        // When
        val result = repository.disableLink(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN no internet WHEN disabling the link THEN fails`() = runTest {
        // Given
        prepareScenario(remoteError = IllegalStateException("offline"))

        // When
        val result = repository.disableLink(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN an open bill with photos WHEN loading it THEN signs every photo for 2 hours`() = runTest {
        // Given
        prepareScenario(sharedBill = openDto(attachments = listOf(attachmentDto("photo-1"), attachmentDto("photo-2"))))

        // When
        repository.fetchSharedBill(CODE)

        // Then
        assertEquals(listOf(listOf(path("photo-1"), path("photo-2")) to 2.hours), attachmentRemote.signRequests)
        assertEquals(listOf(CODE), remote.fetchedCodes)
    }

    @Test
    fun `GIVEN an open bill with photos WHEN loading it THEN reads each photo with its signed image`() = runTest {
        // Given
        prepareScenario(sharedBill = openDto(attachments = listOf(attachmentDto("photo-1"), attachmentDto("photo-2"))))

        // When
        val result = repository.fetchSharedBill(CODE)

        // Then
        assertEquals(
            listOf("photo-1" to signed("photo-1"), "photo-2" to signed("photo-2")),
            assertIs<SharedBill.Open>(result.getOrThrow()).attachments.map { it.id to it.imageUrl },
        )
    }

    @Test
    fun `GIVEN a photo the bucket cannot sign WHEN loading the bill THEN reads that photo with no image`() = runTest {
        // Given
        prepareScenario(
            sharedBill = openDto(attachments = listOf(attachmentDto("photo-1"), attachmentDto("photo-2"))),
            unsignablePaths = setOf(path("photo-2")),
        )

        // When
        val result = repository.fetchSharedBill(CODE)

        // Then
        assertEquals(
            listOf("photo-1" to signed("photo-1"), "photo-2" to null),
            assertIs<SharedBill.Open>(result.getOrThrow()).attachments.map { it.id to it.imageUrl },
        )
    }

    @Test
    fun `GIVEN the bucket fails to sign WHEN loading the bill THEN still reads it with no images`() = runTest {
        // Given
        prepareScenario(
            sharedBill = openDto(attachments = listOf(attachmentDto("photo-1"), attachmentDto("photo-2"))),
            signError = IllegalStateException("storage down"),
        )

        // When
        val result = repository.fetchSharedBill(CODE)

        // Then
        val open = assertIs<SharedBill.Open>(result.getOrThrow())
        assertEquals(listOf("photo-1" to null, "photo-2" to null), open.attachments.map { it.id to it.imageUrl })
    }

    @Test
    fun `GIVEN an open bill without photos WHEN loading it THEN signs nothing`() = runTest {
        // Given
        prepareScenario(sharedBill = openDto(attachments = emptyList()))

        // When
        val result = repository.fetchSharedBill(CODE)

        // Then
        assertIs<SharedBill.Open>(result.getOrThrow())
        assertTrue(attachmentRemote.signRequests.isEmpty())
    }

    @Test
    fun `GIVEN a code that is no link WHEN loading the bill THEN is not found and signs nothing`() = runTest {
        // Given
        prepareScenario(sharedBill = statusOnlyDto("not_found"))

        // When
        val result = repository.fetchSharedBill(CODE)

        // Then
        assertEquals(SharedBill.NotFound, result.getOrThrow())
        assertTrue(attachmentRemote.signRequests.isEmpty())
    }

    @Test
    fun `GIVEN a link only members open WHEN someone outside loads the bill THEN is members only and signs nothing`() =
        runTest {
            // Given
            prepareScenario(sharedBill = statusOnlyDto("members_only"))

            // When
            val result = repository.fetchSharedBill(CODE)

            // Then
            assertEquals(SharedBill.MembersOnly, result.getOrThrow())
            assertTrue(attachmentRemote.signRequests.isEmpty())
        }

    @Test
    fun `GIVEN no internet WHEN loading the bill THEN fails`() = runTest {
        // Given
        prepareScenario(remoteError = IllegalStateException("offline"))

        // When
        val result = repository.fetchSharedBill(CODE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a reply WHEN commenting through the link THEN sends every field as given`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.createSharedBillComment(
            code = CODE,
            id = "comment-9",
            body = "Já paguei",
            threadId = "comment-1",
            replyToName = "Bruno",
            authorName = "Diego",
        )

        // Then
        assertEquals(
            listOf(
                CommentOnSharedBillParamsDto(
                    code = CODE,
                    id = "comment-9",
                    body = "Já paguei",
                    threadId = "comment-1",
                    replyToName = "Bruno",
                    authorName = "Diego",
                ),
            ),
            remote.sentComments,
        )
    }

    @Test
    fun `GIVEN the server takes the comment WHEN commenting through the link THEN is posted`() = runTest {
        // Given
        prepareScenario(commentStatus = "ok")

        // When
        val result = comment()

        // Then
        assertEquals(SharedBillComment.POSTED, result.getOrThrow())
    }

    @Test
    fun `GIVEN the server knows no name WHEN commenting through the link THEN needs a name`() = runTest {
        // Given
        prepareScenario(commentStatus = "needs_name")

        // When
        val result = comment()

        // Then
        assertEquals(SharedBillComment.NEEDS_NAME, result.getOrThrow())
    }

    @Test
    fun `GIVEN no internet WHEN commenting through the link THEN fails`() = runTest {
        // Given
        prepareScenario(remoteError = IllegalStateException("offline"))

        // When
        val result = comment()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private suspend fun comment(): Result<SharedBillComment> = repository.createSharedBillComment(
        code = CODE,
        id = "comment-9",
        body = "Valeu",
        threadId = null,
        replyToName = null,
        authorName = null,
    )

    private fun path(id: String): String = "$GROUP_ID/$BILL_ID/$id.jpg"

    private fun signed(id: String): String = "https://signed/${path(id)}"

    private fun attachmentDto(id: String): SharedBillAttachmentDto = SharedBillAttachmentDto(
        id = id,
        kind = "photo",
        path = path(id),
        width = 1200,
        height = 1600,
        addedByName = "Ana",
        addedAt = 2_000L,
    )

    private fun statusOnlyDto(status: String): SharedBillDto = SharedBillDto(
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

    private fun openDto(attachments: List<SharedBillAttachmentDto>): SharedBillDto = SharedBillDto(
        status = "ok",
        isMember = false,
        canComment = true,
        groupId = GROUP_ID,
        groupName = "Viagem Floripa",
        currency = "BRL",
        visibility = "anyone",
        bill = SharedBillExpenseDto(
            id = BILL_ID,
            description = "Jantar",
            amountMinor = 31_200L,
            spentAt = 1_000L,
            categoryId = null,
            splitMode = "equal",
            payers = emptyList(),
            shares = emptyList(),
        ),
        attachments = attachments,
        comments = emptyList(),
    )

    private fun prepareScenario(
        storedLink: BillShareLinkDto? = null,
        updateAnswer: BillLinkResultDto = BillLinkResultDto(
            status = "ok",
            code = CODE,
            visibility = "anyone",
        ),
        disableAnswer: BillLinkResultDto = BillLinkResultDto(
            status = "ok",
            code = null,
            visibility = null,
        ),
        sharedBill: SharedBillDto = statusOnlyDto("not_found"),
        commentStatus: String = "ok",
        remoteError: Throwable? = null,
        unsignablePaths: Set<String> = emptySet(),
        signError: Throwable? = null,
    ) {
        remote = FakeBillShareLinkRemoteDataSource(
            storedLink = storedLink,
            updateAnswer = updateAnswer,
            disableAnswer = disableAnswer,
            sharedBill = sharedBill,
            commentStatus = commentStatus,
            error = remoteError,
        )
        attachmentRemote = FakeSigningRemoteDataSource(
            unsignablePaths = unsignablePaths,
            signError = signError,
        )
        repository = BillShareLinkRepositoryImpl(
            remote = remote,
            attachmentRemote = attachmentRemote,
            mapper = SharedBillMapperImpl(),
        )
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val GROUP_ID = "group-1"
        const val BILL_ID = "bill-1"
    }
}

private class FakeBillShareLinkRemoteDataSource(
    private val storedLink: BillShareLinkDto?,
    private val updateAnswer: BillLinkResultDto,
    private val disableAnswer: BillLinkResultDto,
    private val sharedBill: SharedBillDto,
    private val commentStatus: String,
    private val error: Throwable?,
) : BillShareLinkRemoteDataSource {
    val fetchedLinkIds = mutableListOf<String>()
    val updates = mutableListOf<Pair<String, String>>()
    val disabledIds = mutableListOf<String>()
    val fetchedCodes = mutableListOf<String>()
    val sentComments = mutableListOf<CommentOnSharedBillParamsDto>()

    override suspend fun fetchLink(expenseId: String): BillShareLinkDto? {
        error?.let { throw it }
        fetchedLinkIds += expenseId
        return storedLink
    }

    override suspend fun updateLink(
        expenseId: String,
        visibility: String,
    ): BillLinkResultDto {
        error?.let { throw it }
        updates += expenseId to visibility
        return updateAnswer
    }

    override suspend fun disableLink(expenseId: String): BillLinkResultDto {
        error?.let { throw it }
        disabledIds += expenseId
        return disableAnswer
    }

    override suspend fun fetchSharedBill(code: String): SharedBillDto {
        error?.let { throw it }
        fetchedCodes += code
        return sharedBill
    }

    override suspend fun createSharedBillComment(params: CommentOnSharedBillParamsDto): SharedBillStatusDto {
        error?.let { throw it }
        sentComments += params
        return SharedBillStatusDto(status = commentStatus)
    }
}

private class FakeSigningRemoteDataSource(
    private val unsignablePaths: Set<String>,
    private val signError: Throwable?,
) : BillAttachmentRemoteDataSource {
    val signRequests = mutableListOf<Pair<List<String>, Duration>>()

    override suspend fun uploadImage(
        path: String,
        jpeg: ByteArray,
    ) = error("unused")

    override suspend fun removeImage(path: String) = error("unused")

    override suspend fun upsertAttachment(attachment: BillAttachmentDto) = error("unused")

    override suspend fun fetchAttachments(): List<BillAttachmentDto> = error("unused")

    override suspend fun createSignedUrls(
        paths: List<String>,
        expiresIn: Duration,
    ): Map<String, String> {
        signRequests += paths to expiresIn
        signError?.let { throw it }
        return paths.filterNot { it in unsignablePaths }.associateWith { "https://signed/$it" }
    }
}
