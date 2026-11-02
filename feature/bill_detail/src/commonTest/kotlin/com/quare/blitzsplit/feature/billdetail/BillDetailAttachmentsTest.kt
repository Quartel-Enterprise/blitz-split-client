package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.activity.domain.usecase.impl.LoadBillHistoryUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveBillHistoryUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillAttachment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.AddBillCommentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadBillAttachmentImageUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadBillAttachmentUrlsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillAttachmentsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillCommentsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RemoveBillAttachmentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// Conta's "Anexos" (7.3g): the tiles, the signed URLs, adding and removing a photo and the viewer.
class BillDetailAttachmentsTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var attachments: FakeBillAttachmentRepository
    private val isOnline = MutableStateFlow(true)
    private val minutes = MutableSharedFlow<Unit>()
    private lateinit var viewModel: BillDetailViewModel
    private val actions = mutableListOf<BillDetailUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the receipt and two photos WHEN the bill opens THEN lists the receipt then Foto 1 and Foto 2`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(receipt, photoA, photoB))

            // When
            val tiles = viewModel.uiState.value.attachments

            // Then
            assertEquals(listOf(RECEIPT, PHOTO_A, PHOTO_B), tiles.map { it.id })
            assertEquals(listOf(true, false, false), tiles.map { it.isReceipt })
            assertEquals(listOf(null, 1, 2), tiles.map { it.photoNumber })
        }

    @Test
    fun `GIVEN photos of another bill WHEN the bill opens THEN lists only the photos of this one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(
                    photoA,
                    attachment(
                        id = "other",
                        expenseId = "expense-2",
                    ),
                ),
            )

            // When
            val tiles = viewModel.uiState.value.attachments

            // Then
            assertEquals(listOf(PHOTO_A), tiles.map { it.id })
        }

    @Test
    fun `GIVEN Room not read yet WHEN the bill opens THEN shows no attachment and a photo cannot be added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                isRoomRead = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.attachments.isEmpty())
            assertFalse(state.canAddPhoto)
        }

    @Test
    fun `GIVEN a photo by Ana and one by the device user WHEN the bill opens THEN says who added each`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(
                    attachment(
                        id = PHOTO_A,
                        addedByMemberId = ANA,
                    ),
                    attachment(
                        id = PHOTO_B,
                        addedByMemberId = PIERRE,
                    ),
                ),
            )

            // When
            val tiles = viewModel.uiState.value.attachments

            // Then
            assertEquals(listOf("Ana", "Pierre"), tiles.map { it.adderName })
            assertEquals(listOf(false, true), tiles.map { it.isAddedByMe })
        }

    @Test
    fun `GIVEN a photo by someone who left the group WHEN the bill opens THEN it has no name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(attachment(addedByMemberId = FORMER)))

            // When
            val tile = viewModel.uiState.value.attachments
                .single()

            // Then
            assertNull(tile.adderName)
            assertFalse(tile.isAddedByMe)
        }

    @Test
    fun `GIVEN a photo by Ana WHEN a sync renames her THEN the photo shows her new name`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(attachment(addedByMemberId = ANA)))
        val renamed = trip.copy(members = listOf(ana.copy(name = "Ana Paula"), pierre, bruno))

        // When
        groups.groups.value = listOf(renamed)

        // Then
        assertEquals(
            "Ana Paula",
            viewModel.uiState.value.attachments
                .single()
                .adderName,
        )
    }

    @Test
    fun `GIVEN a photo added today WHEN midnight passes with the bill open THEN it reads as yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(attachment(addedAt = earlierToday)))
            assertEquals(
                RelativeDay.TODAY,
                viewModel.uiState.value.attachments
                    .single()
                    .addedDay,
            )
            nowMillis += 1.days.inWholeMilliseconds

            // When
            minutes.emit(Unit)

            // Then
            assertEquals(
                RelativeDay.YESTERDAY,
                viewModel.uiState.value.attachments
                    .single()
                    .addedDay,
            )
        }

    @Test
    fun `GIVEN synced photos online WHEN the bill opens THEN asks their signed URLs and draws them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA, photoB),
                imageUrls = mapOf(PHOTO_A to URL_A, PHOTO_B to URL_B),
            )

            // When
            val tiles = viewModel.uiState.value.attachments

            // Then
            assertEquals(listOf(listOf(PHOTO_A, PHOTO_B)), attachments.urlRequests)
            assertEquals(listOf(URL_A, URL_B), tiles.map { it.imageUrl })
        }

    @Test
    fun `GIVEN synced photos offline WHEN the bill opens THEN asks no URL and draws no image`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                imageUrls = mapOf(PHOTO_A to URL_A),
                online = false,
            )

            // When
            val tile = viewModel.uiState.value.attachments
                .single()

            // Then
            assertTrue(attachments.urlRequests.isEmpty())
            assertNull(tile.imageUrl)
        }

    @Test
    fun `GIVEN the bill open offline with a synced photo WHEN the internet comes back THEN draws its signed URL`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                imageUrls = mapOf(PHOTO_A to URL_A),
                online = false,
            )

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(listOf(PHOTO_A)), attachments.urlRequests)
            assertEquals(
                URL_A,
                viewModel.uiState.value.attachments
                    .single()
                    .imageUrl,
            )
        }

    @Test
    fun `GIVEN only photos added here not up yet WHEN the bill opens online THEN asks no URL and draws their JPEG`() =
        runTest(testDispatcher) {
            // Given
            val jpeg = byteArrayOf(1, 2, 3)
            prepareScenario(
                storedAttachments = listOf(
                    attachment(
                        id = PHOTO_A,
                        isPending = true,
                        hasPendingImage = true,
                    ),
                ),
                pendingImages = mapOf(PHOTO_A to jpeg),
            )

            // When
            val tile = viewModel.uiState.value.attachments
                .single()

            // Then
            assertTrue(attachments.urlRequests.isEmpty())
            assertTrue(jpeg.contentEquals(tile.localImage))
            assertNull(tile.imageUrl)
            assertTrue(tile.isPending)
        }

    @Test
    fun `GIVEN photos drawn WHEN a minute passes and the repository signs them again THEN draws the new URLs`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                imageUrls = mapOf(PHOTO_A to URL_A),
            )
            attachments.imageUrls = mapOf(PHOTO_A to "https://signed/a-again")

            // When
            minutes.emit(Unit)

            // Then
            assertEquals(2, attachments.urlRequests.size)
            assertEquals(
                "https://signed/a-again",
                viewModel.uiState.value.attachments
                    .single()
                    .imageUrl,
            )
        }

    @Test
    fun `GIVEN the signed URLs failed WHEN a minute passes THEN tries again and draws them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                imageUrls = mapOf(PHOTO_A to URL_A),
                urlsError = IllegalStateException("bucket unreachable"),
            )
            attachments.urlsError = null

            // When
            minutes.emit(Unit)

            // Then
            assertEquals(2, attachments.urlRequests.size)
            assertEquals(
                URL_A,
                viewModel.uiState.value.attachments
                    .single()
                    .imageUrl,
            )
        }

    @Test
    fun `GIVEN photos offline WHEN a minute passes THEN asks no URL`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedAttachments = listOf(photoA),
            imageUrls = mapOf(PHOTO_A to URL_A),
            online = false,
        )

        // When
        minutes.emit(Unit)

        // Then
        assertTrue(attachments.urlRequests.isEmpty())
    }

    @Test
    fun `GIVEN a photo drawn WHEN a sync brings another synced photo THEN asks again and draws the new one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                imageUrls = mapOf(PHOTO_A to URL_A, PHOTO_B to URL_B),
            )

            // When
            attachments.attachments.value = listOf(photoA, photoB)

            // Then
            assertEquals(2, attachments.urlRequests.size)
            assertEquals(
                listOf(URL_A, URL_B),
                viewModel.uiState.value.attachments
                    .map { it.imageUrl },
            )
        }

    @Test
    fun `GIVEN a photo added here WHEN the sync takes it up THEN keeps drawing its JPEG without asking its URL`() =
        runTest(testDispatcher) {
            // Given
            val jpeg = byteArrayOf(1, 2, 3)
            val pending = attachment(
                id = PHOTO_A,
                isPending = true,
                hasPendingImage = true,
            )
            prepareScenario(
                storedAttachments = listOf(pending),
                imageUrls = mapOf(PHOTO_A to URL_A),
                pendingImages = mapOf(PHOTO_A to jpeg),
            )

            // When
            attachments.attachments.value = listOf(
                pending.copy(
                    isPending = false,
                    hasPendingImage = false,
                ),
            )

            // Then
            // Why: no placeholder blinks while a signed URL comes, nor a second download of the same photo.
            val tile = viewModel.uiState.value.attachments
                .single()
            assertTrue(jpeg.contentEquals(tile.localImage))
            assertEquals(listOf(PHOTO_A), attachments.imageRequests)
            assertFalse(tile.isPending)
            assertTrue(attachments.urlRequests.isEmpty())
        }

    @Test
    fun `GIVEN the signed URLs fail WHEN the bill opens online THEN draws no image and says nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                imageUrls = mapOf(PHOTO_A to URL_A),
                urlsError = IllegalStateException("bucket unreachable"),
            )

            // When
            val tile = viewModel.uiState.value.attachments
                .single()

            // Then
            assertEquals(1, attachments.urlRequests.size)
            assertNull(tile.imageUrl)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the receipt and two photos WHEN the bill opens THEN counts two of five photos and one can be added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(receipt, photoA, photoB))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(2, state.photoCount)
            assertEquals(5, state.photoLimit)
            assertTrue(state.showsAddPhoto)
            assertTrue(state.canAddPhoto)
        }

    @Test
    fun `GIVEN a bill without attachments WHEN it opens offline THEN counts no photo and one can be added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(0, state.photoCount)
            assertTrue(state.canAddPhoto)
        }

    @Test
    fun `GIVEN the receipt and five photos WHEN the bill opens THEN no more photo can be added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(receipt) + fivePhotos)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(5, state.photoCount)
            assertFalse(state.canAddPhoto)
            assertFalse(state.showsAddPhoto)
        }

    @Test
    fun `GIVEN four photos WHEN a sync brings a fifth THEN no more photo can be added`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = fivePhotos.take(4))

        // When
        attachments.attachments.value = fivePhotos

        // Then
        assertFalse(viewModel.uiState.value.canAddPhoto)
    }

    @Test
    fun `GIVEN the bill is being deleted WHEN reading Anexos THEN no photo can be added`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedAttachments = listOf(photoA),
            deleteGate = CompletableDeferred(),
        )
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsAddPhoto)
        assertFalse(state.canAddPhoto)
    }

    @Test
    fun `GIVEN the bill is open WHEN picking a photo THEN adds it by the device user to this bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val picked = byteArrayOf(9, 8, 7)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(picked))

            // Then
            val added = attachments.attachments.value
                .single()
            assertEquals(BillAttachmentKind.PHOTO, added.kind)
            assertEquals(EXPENSE_ID, added.expenseId)
            assertEquals(GROUP_ID, added.groupId)
            assertEquals(PIERRE, added.addedByMemberId)
            assertTrue(added.hasPendingImage)
            assertTrue(picked.contentEquals(attachments.pendingImages[added.id]))
            assertFalse(viewModel.uiState.value.isAddingPhoto)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the bill is open WHEN picking a photo THEN shows it as Foto 1 by the device user waiting to go up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val picked = byteArrayOf(9, 8, 7)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(picked))

            // Then
            val state = viewModel.uiState.value
            val tile = state.attachments.single()
            assertEquals(1, tile.photoNumber)
            assertTrue(tile.isAddedByMe)
            assertEquals("Pierre", tile.adderName)
            assertTrue(tile.isPending)
            assertTrue(picked.contentEquals(tile.localImage))
            assertEquals(RelativeDay.TODAY, tile.addedDay)
            assertEquals(1, state.photoCount)
            assertTrue(attachments.urlRequests.isEmpty())
        }

    @Test
    fun `GIVEN no internet WHEN picking a photo THEN still adds it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // Then
        assertEquals(1, attachments.attachments.value.size)
        assertEquals(1, viewModel.uiState.value.photoCount)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the photo is being made ready WHEN reading Anexos THEN it is adding and no other can be added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(addGate = CompletableDeferred())

            // When
            viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isAddingPhoto)
            assertFalse(state.canAddPhoto)
        }

    @Test
    fun `GIVEN a photo being made ready WHEN it is kept THEN is no longer adding`() = runTest(testDispatcher) {
        // Given
        val gate = CompletableDeferred<Unit>()
        prepareScenario(addGate = gate)
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // When
        gate.complete(Unit)

        // Then
        assertFalse(viewModel.uiState.value.isAddingPhoto)
        assertEquals(1, attachments.attachments.value.size)
    }

    @Test
    fun `GIVEN a photo being made ready WHEN picking another THEN only the first is added`() = runTest(testDispatcher) {
        // Given
        prepareScenario(addGate = CompletableDeferred())
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(2)))

        // Then
        assertEquals(1, additions)
        assertTrue(viewModel.uiState.value.isAddingPhoto)
    }

    @Test
    fun `GIVEN five photos WHEN picking another THEN nothing is added and nothing is said`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = fivePhotos)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // Then
        assertEquals(0, additions)
        assertEquals(5, attachments.attachments.value.size)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Room not read yet WHEN picking a photo THEN nothing is added`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // Then
        assertEquals(0, additions)
    }

    @Test
    fun `GIVEN the bill is being deleted WHEN picking a photo THEN nothing is added`() = runTest(testDispatcher) {
        // Given
        prepareScenario(deleteGate = CompletableDeferred())
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // Then
        assertEquals(0, additions)
    }

    @Test
    fun `GIVEN another device filled the five photos WHEN picking a photo THEN says the bill has no room`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(hasRoom = false)

            // When
            viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

            // Then
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowPhotoError(BillPhotoError.NO_ROOM)), actions)
            assertFalse(viewModel.uiState.value.isAddingPhoto)
            assertTrue(attachments.attachments.value.isEmpty())
        }

    @Test
    fun `GIVEN a file that is no image WHEN picking it THEN says it cannot be opened`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isUnreadable = true)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // Then
        assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowPhotoError(BillPhotoError.UNREADABLE)), actions)
        assertFalse(viewModel.uiState.value.isAddingPhoto)
        assertTrue(attachments.attachments.value.isEmpty())
    }

    @Test
    fun `GIVEN the device cannot keep the photo WHEN picking it THEN says it failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(addError = IllegalStateException("bill gone"))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(byteArrayOf(1)))

        // Then
        assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowPhotoError(BillPhotoError.FAILED)), actions)
        assertFalse(viewModel.uiState.value.isAddingPhoto)
    }

    @Test
    fun `GIVEN the platform could not read the file WHEN it says so THEN says the image cannot be opened`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(BillDetailUiEvent.OnPhotoUnreadable)

            // Then
            assertEquals(
                listOf<BillDetailUiAction>(BillDetailUiAction.ShowPhotoError(BillPhotoError.UNREADABLE)),
                actions,
            )
            assertEquals(0, additions)
        }

    @Test
    fun `GIVEN the bill opens WHEN reading the viewer THEN it is closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(receipt, photoA))

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.viewedAttachmentId)
        assertNull(state.viewedAttachmentIndex)
        assertNull(state.viewedAttachment)
    }

    @Test
    fun `GIVEN the receipt and two photos WHEN tapping Foto 2 THEN the viewer opens on it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(receipt, photoA, photoB))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_B))

        // Then
        val state = viewModel.uiState.value
        assertEquals(PHOTO_B, state.viewedAttachmentId)
        assertEquals(2, state.viewedAttachmentIndex)
        assertEquals(PHOTO_B, state.viewedAttachment?.id)
    }

    @Test
    fun `GIVEN the bill is open WHEN tapping an attachment that is not there THEN the viewer stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(photoA))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked("gone"))

            // Then
            assertNull(viewModel.uiState.value.viewedAttachmentId)
        }

    @Test
    fun `GIVEN the viewer on the receipt WHEN swiping to Foto 1 THEN shows Foto 1`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(receipt, photoA, photoB))
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(RECEIPT))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnViewerPageChanged(PHOTO_A))

        // Then
        val state = viewModel.uiState.value
        assertEquals(PHOTO_A, state.viewedAttachmentId)
        assertEquals(1, state.viewedAttachmentIndex)
    }

    @Test
    fun `GIVEN the viewer on Foto 1 WHEN swiping to an attachment that is not there THEN stays on Foto 1`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(photoA, photoB))
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_A))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnViewerPageChanged("gone"))

            // Then
            assertEquals(PHOTO_A, viewModel.uiState.value.viewedAttachmentId)
        }

    @Test
    fun `GIVEN the viewer open WHEN dismissing it THEN it closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(photoA))
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_A))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnViewerDismissed)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.viewedAttachmentId)
        assertNull(state.viewedAttachment)
    }

    @Test
    fun `GIVEN the viewer on Foto 2 WHEN another device removes it THEN the viewer has nothing to show`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(photoA, photoB))
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_B))

            // When
            attachments.attachments.value = listOf(photoA)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.viewedAttachmentIndex)
            assertNull(state.viewedAttachment)
        }

    @Test
    fun `GIVEN the viewer on Foto 2 WHEN another device removes Foto 1 THEN still shows Foto 2 one page earlier`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(photoA, photoB))
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_B))

            // When
            attachments.attachments.value = listOf(photoB)

            // Then
            val state = viewModel.uiState.value
            assertEquals(0, state.viewedAttachmentIndex)
            assertEquals(PHOTO_B, state.viewedAttachment?.id)
        }

    @Test
    fun `GIVEN a photo WHEN removing it THEN it leaves the bill with no question`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(receipt, photoA, photoB))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

        // Then
        assertEquals(listOf(PHOTO_A to nowMillis), attachments.removals)
        assertEquals(
            listOf(RECEIPT, PHOTO_B),
            viewModel.uiState.value.attachments
                .map { it.id },
        )
        assertEquals(
            listOf(null, 1),
            viewModel.uiState.value.attachments
                .map { it.photoNumber },
        )
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN no internet WHEN removing a photo THEN still removes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedAttachments = listOf(photoA),
            online = false,
        )

        // When
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

        // Then
        assertEquals(listOf(PHOTO_A), attachments.removedIds)
        assertTrue(
            viewModel.uiState.value.attachments
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN the viewer on Foto 1 of two WHEN removing it THEN the viewer goes on to Foto 2`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(receipt, photoA, photoB))
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_A))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

            // Then
            val state = viewModel.uiState.value
            assertEquals(PHOTO_B, state.viewedAttachmentId)
            assertEquals(1, state.viewedAttachmentIndex)
        }

    @Test
    fun `GIVEN the viewer on the last photo WHEN removing it THEN the viewer goes back to the one before`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(photoA, photoB))
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_B))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_B))

            // Then
            val state = viewModel.uiState.value
            assertEquals(PHOTO_A, state.viewedAttachmentId)
            assertEquals(0, state.viewedAttachmentIndex)
        }

    @Test
    fun `GIVEN the viewer on the only photo after the receipt WHEN removing it THEN the viewer goes to the receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(receipt, photoA))
            viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_A))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

            // Then
            assertEquals(RECEIPT, viewModel.uiState.value.viewedAttachmentId)
        }

    @Test
    fun `GIVEN the viewer on the only attachment WHEN removing it THEN the viewer closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(photoA))
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_A))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.viewedAttachmentId)
        assertNull(state.viewedAttachment)
    }

    @Test
    fun `GIVEN the viewer on Foto 2 WHEN removing Foto 1 THEN the viewer stays on Foto 2`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(photoA, photoB))
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_B))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

        // Then
        assertEquals(PHOTO_B, viewModel.uiState.value.viewedAttachmentId)
        assertEquals(listOf(PHOTO_A), attachments.removedIds)
    }

    @Test
    fun `GIVEN the receipt WHEN removing it THEN it stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedAttachments = listOf(receipt, photoA))
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(RECEIPT))

        // When
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(RECEIPT))

        // Then
        assertTrue(attachments.removedIds.isEmpty())
        assertEquals(RECEIPT, viewModel.uiState.value.viewedAttachmentId)
        assertEquals(
            listOf(RECEIPT, PHOTO_A),
            viewModel.uiState.value.attachments
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN the bill is open WHEN removing an attachment that is not there THEN nothing is removed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedAttachments = listOf(photoA))

            // When
            viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked("gone"))

            // Then
            assertTrue(attachments.removedIds.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the bill is being deleted WHEN removing a photo THEN nothing is removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedAttachments = listOf(photoA),
            deleteGate = CompletableDeferred(),
        )
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(PHOTO_A))
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)

        // When
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

        // Then
        assertTrue(attachments.removedIds.isEmpty())
        assertEquals(PHOTO_A, viewModel.uiState.value.viewedAttachmentId)
    }

    @Test
    fun `GIVEN the device cannot remove the photo WHEN removing it THEN keeps it and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedAttachments = listOf(photoA),
                removeError = IllegalStateException("disk full"),
            )

            // When
            viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(PHOTO_A))

            // Then
            assertEquals(listOf<BillDetailUiAction>(BillDetailUiAction.ShowRemovePhotoError), actions)
            assertEquals(
                listOf(PHOTO_A),
                viewModel.uiState.value.attachments
                    .map { it.id },
            )
        }

    private val route = Route.BillDetail(
        groupId = GROUP_ID,
        expenseId = EXPENSE_ID,
    )
    private val groupRoute = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    private var nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val earlierToday = Instant.parse("2026-10-30T12:00:00Z").toEpochMilliseconds()

    // Why: how many photos the add use case was asked to keep, whatever came of it.
    private var additions = 0

    private val ana = member(ANA, "Ana")
    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val bruno = member(BRUNO, "Bruno")

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
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

    // Why: Ana paid R$ 300 split equally among the three.
    private val bill = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 30_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = ANA,
                amountMinor = 30_000,
            ),
        ),
        shares = listOf(ANA, PIERRE, BRUNO).map {
            ExpenseShare(
                memberId = it,
                amountMinor = 10_000,
                weight = 1,
            )
        },
        items = null,
        categoryId = null,
        spentAt = earlierToday,
        isPending = false,
        updatedAt = earlierToday,
        updatedByMemberId = null,
    )

    private fun attachment(
        id: String = PHOTO_A,
        kind: BillAttachmentKind = BillAttachmentKind.PHOTO,
        expenseId: String = EXPENSE_ID,
        addedByMemberId: String? = ANA,
        addedAt: Long = earlierToday,
        isPending: Boolean = false,
        hasPendingImage: Boolean = false,
    ): BillAttachment = BillAttachment(
        id = id,
        expenseId = expenseId,
        groupId = GROUP_ID,
        kind = kind,
        width = 1600,
        height = 1200,
        addedByUserId = null,
        addedByMemberId = addedByMemberId,
        addedAt = addedAt,
        updatedAt = addedAt,
        isPending = isPending,
        hasPendingImage = hasPendingImage,
    )

    private val receipt = attachment(
        id = RECEIPT,
        kind = BillAttachmentKind.RECEIPT,
    )
    private val photoA = attachment(id = PHOTO_A)
    private val photoB = attachment(id = PHOTO_B)
    private val fivePhotos = (1..5).map { attachment(id = "photo-$it") }

    private fun TestScope.prepareScenario(
        storedAttachments: List<BillAttachment> = emptyList(),
        isRoomRead: Boolean = true,
        online: Boolean = true,
        imageUrls: Map<String, String> = emptyMap(),
        pendingImages: Map<String, ByteArray> = emptyMap(),
        urlsError: Throwable? = null,
        hasRoom: Boolean = true,
        isUnreadable: Boolean = false,
        addError: Throwable? = null,
        addGate: CompletableDeferred<Unit>? = null,
        removeError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
    ) {
        isOnline.value = online
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = listOf(bill)).also { it.deleteGate = deleteGate }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupRoute)
            it.navigate(route)
        }
        attachments = FakeBillAttachmentRepository(initialAttachments = storedAttachments).also {
            it.imageUrls = imageUrls
            it.pendingImages += pendingImages
            it.urlsError = urlsError
            it.hasRoom = hasRoom
            it.addError = addError
            it.removeError = removeError
        }
        val deletedBills = InMemoryDeletedBillRepository()
        val comments = FakeBillCommentRepository()
        val activity = FakeActivityRepository()
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val expenseNeverAnswers = MutableSharedFlow<Expense?>()
        viewModel = BillDetailViewModel(
            route = route,
            navigator = navigator,
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills, currentTimeMillis = { nowMillis }),
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { nowMillis },
            currentTimeZone = { TimeZone.UTC },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
            observeExpense = if (isRoomRead) {
                ObserveExpenseUseCase(
                    expenses,
                )
            } else {
                ObserveExpense { expenseNeverAnswers }
            },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(FakeSettlementRepository()),
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            addBillComment = AddBillCommentUseCase(comments, currentTimeMillis = { nowMillis }),
            updateExpense = UpdateExpenseUseCase(expenses, currentTimeMillis = { nowMillis }),
            loadBillHistory = LoadBillHistoryUseCase(activity),
            observeBillComments = ObserveBillCommentsUseCase(comments),
            observeBillHistory = ObserveBillHistoryUseCase(activity),
            // Why: the real use case decodes on the platform; this one keeps the picked bytes as the JPEG.
            addBillAttachment = AddBillAttachment { expenseId, groupId, kind, source, memberId ->
                additions++
                addGate?.await()
                if (isUnreadable) return@AddBillAttachment Result.success(BillAttachmentAddition.UNREADABLE)
                attachments
                    .addAttachment(
                        BillAttachment(
                            id = "added-$additions",
                            expenseId = expenseId,
                            groupId = groupId,
                            kind = kind,
                            width = 1600,
                            height = 1200,
                            addedByUserId = null,
                            addedByMemberId = memberId,
                            addedAt = nowMillis,
                            updatedAt = nowMillis,
                            isPending = true,
                            hasPendingImage = true,
                        ),
                        jpeg = source,
                    ).map { added -> if (added) BillAttachmentAddition.ADDED else BillAttachmentAddition.NO_ROOM }
            },
            removeBillAttachment = RemoveBillAttachmentUseCase(attachments, currentTimeMillis = { nowMillis }),
            loadBillAttachmentUrls = LoadBillAttachmentUrlsUseCase(attachments),
            loadBillAttachmentImage = LoadBillAttachmentImageUseCase(attachments),
            observeBillAttachments = ObserveBillAttachmentsUseCase(attachments),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            // Why: at once, then on each minute, as SystemMinuteTicks.
            minuteTicks = MinuteTicks { minutes.onStart { emit(Unit) } },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val FORMER = "member-9"
        const val RECEIPT = "receipt"
        const val PHOTO_A = "photo-a"
        const val PHOTO_B = "photo-b"
        const val URL_A = "https://bucket/photo-a.jpg"
        const val URL_B = "https://bucket/photo-b.jpg"
    }
}
