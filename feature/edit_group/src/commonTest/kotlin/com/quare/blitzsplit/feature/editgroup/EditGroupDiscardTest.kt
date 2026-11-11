package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetSimplifiesDebtsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetSplitIncomesUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateDefaultSplitUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditGroupDiscardTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: EditGroupViewModel
    private lateinit var defaultSplitViewModel: DefaultSplitViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the form as it opened WHEN reading the unsaved input THEN there is none`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val hasUnsavedInput = viewModel.uiState.value.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a new name WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN the same name with spaces around it WHEN reading the unsaved input THEN there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("  Viagem Floripa "))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN a new type WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(GroupType.EVENT))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new color WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.SAND))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new currency WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new photo WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnPhotoPicked(byteArrayOf(1, 2, 3)))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a name changed and typed back WHEN reading the unsaved input THEN there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Viagem Floripa"))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN Room not read yet WHEN a name is typed THEN there is no unsaved input`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // Then
        assertFalse(viewModel.uiState.value.hasUnsavedInput)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN Simplificar dividas turned on WHEN reading the unsaved input THEN there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // Then
            assertTrue(groups.simplifyDebtsChanges.isNotEmpty())
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN a new name and Simplificar dividas turned on WHEN reading the unsaved input THEN the name counts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a new name WHEN closing THEN asks to discard and the form stays open`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertTrue(navigator.pendingDiscard.value)
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a new name WHEN going back natively THEN asks to discard and the form stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            navigator.navigateBack()

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the discard question WHEN confirming THEN the form closes without saving`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // When
        navigator.confirmDiscard()

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertTrue(groups.updatedGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN the discard question WHEN cancelling THEN the form stays open with what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

            // When
            navigator.cancelDiscard()

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
            assertFalse(navigator.pendingDiscard.value)
            assertEquals("Churrasco", viewModel.uiState.value.name)
            assertTrue(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN the discard question cancelled WHEN closing again THEN asks again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)
        navigator.cancelDiscard()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertTrue(navigator.pendingDiscard.value)
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the form as it opened WHEN closing THEN closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a name changed and typed back WHEN closing THEN closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Viagem Floripa"))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a new name WHEN saving THEN leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(navigator.hasUnsavedInput.value)
        assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
    }

    @Test
    fun `GIVEN a new name and a save that fails WHEN saving THEN stays open and still has unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateError = IllegalStateException("no disk"))
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.showsSaveError)
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
            assertTrue(navigator.hasUnsavedInput.value)
            assertFalse(navigator.pendingDiscard.value)
        }

    @Test
    fun `GIVEN a new name WHEN the group is deleted THEN leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN a new name and the default split on top WHEN the default split closes THEN it is not held`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(withDefaultSplitOver = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            defaultSplitViewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
            assertFalse(navigator.pendingDiscard.value)
            assertTrue(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN a new name and the default split on top WHEN the form closes under it THEN asks to discard`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(withDefaultSplitOver = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            navigator.popTo(2)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, groupDetail, editGroup, defaultSplit), navigator.backStack.value)
        }

    private val editGroup = Route.EditGroup(GROUP_ID)

    private val defaultSplit = Route.DefaultSplit(GROUP_ID)

    private val groupDetail = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
        isOverBill = false,
    )

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(PIERRE, "Pierre").copy(
                isMe = true,
                hasJoined = true,
            ),
            member(ANA, "Ana"),
        ),
        isPending = false,
        inviteCode = "AB12CD",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    // Why: three people, so "Simplificar dívidas" can be turned on.
    private val threePeople = trip.copy(members = trip.members + member(BRUNO, "Bruno"))

    // Why: Ana owes Bruno R$ 30, and with pierrePaidForBruno Bruno owes Pierre R$ 30: one payment, Ana to Pierre.
    private val brunoPaidForAna = bill("expense-1", BRUNO, mapOf(ANA to 3_000L, BRUNO to 3_000L))

    private val pierrePaidForBruno = bill("expense-2", PIERRE, mapOf(PIERRE to 3_000L, BRUNO to 3_000L))

    private fun member(
        id: String,
        name: String,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.LILAC,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun bill(
        id: String,
        payerId: String,
        shares: Map<String, Long>,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Jantar",
        currency = GroupCurrency.BRL,
        amountMinor = shares.values.sum(),
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(payerId, shares.values.sum())),
        shares = shares.map { (memberId, amountMinor) ->
            ExpenseShare(
                memberId = memberId,
                amountMinor = amountMinor,
                weight = 1,
            )
        },
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        isRoomRead: Boolean = true,
        updateError: Throwable? = null,
        withDefaultSplitOver: Boolean = false,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.updateGroupError = updateError
        }
        val expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        val settlements = FakeSettlementRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupDetail)
            it.navigate(editGroup)
        }
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        viewModel = EditGroupViewModel(
            trackEvent = { _, _ -> },
            route = editGroup,
            navigator = navigator,
            updateGroup = UpdateGroupUseCase(groups) { 1_000L },
            setSimplifiesDebts = SetSimplifiesDebtsUseCase(groups, expenses, settlements) { 1_000L },
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { roomNeverAnswers },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(settlements),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            loadCropPhoto = { source ->
                Result.success(
                    CropPhoto(
                        jpeg = source,
                        width = 1200,
                        height = 900,
                    ),
                )
            },
            prepareProfilePhoto = { _, _ -> Result.success(ProfilePhoto.Pending(byteArrayOf(9, 8, 7))) },
        )
        if (withDefaultSplitOver) {
            navigator.navigate(defaultSplit)
            defaultSplitViewModel = DefaultSplitViewModel(
                trackEvent = { _, _ -> },
                route = defaultSplit,
                navigator = navigator,
                updateDefaultSplit = UpdateDefaultSplitUseCase(groups) { 1_000L },
                getSplitIncomes = GetSplitIncomesUseCase(groups),
                decimalSeparator = { ',' },
                observeGroup = ObserveGroupUseCase(groups),
                observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
                observeBitcoinInSats = { MutableStateFlow(false) },
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            )
        }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
    }
}
