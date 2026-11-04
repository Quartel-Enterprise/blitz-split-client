package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillPart
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CommentOnSharedBillUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSharedBillCommentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.EditSharedBillCommentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LikeSharedBillCommentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadSharedBillUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillStatus
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import software.quare.connectivity.NetworkConnectivityObserver

class SharedBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var links: FakeBillShareLinkRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var profiles: FakeProfileRepository
    private lateinit var navigator: BackStackNavigator
    private val isOnline = MutableStateFlow(true)
    private lateinit var viewModel: SharedBillViewModel
    private val actions = mutableListOf<SharedBillUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN an open link WHEN the page opens online THEN shows the bill and its group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SharedBillStatus.OPEN, state.status)
        assertEquals(listOf(CODE), links.loadedCodes)
        assertEquals("Viagem Floripa", state.groupName)
        assertEquals(GroupCurrency.USD, state.currency)
        assertEquals("Jantar no Ostradamus", state.description)
        assertEquals(64_200, state.totalMinor)
        assertEquals(yesterdayEvening, state.spentAt)
        assertEquals(SplitMode.EQUAL, state.splitMode)
    }

    @Test
    fun `GIVEN a bill of yesterday WHEN the page opens THEN its day is yesterday`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(RelativeDay.YESTERDAY, state.spentDay)
    }

    @Test
    fun `GIVEN a bill of early today in UTC WHEN the page opens three hours behind THEN its day is yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(spentAt = earlyToday),
                timeZone = UtcOffset(hours = -3).asTimeZone(),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(RelativeDay.YESTERDAY, state.spentDay)
        }

    @Test
    fun `GIVEN a bill in Mercado WHEN the page opens THEN shows Alimentacao in Mercado`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(categoryId = AppCategory.FOOD_MARKET.key))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(AppCategory.FOOD_MARKET.key, state.category.id)
        assertEquals(AppCategory.FOOD.key, state.categoryParent?.id)
    }

    @Test
    fun `GIVEN a bill in a category of the group WHEN the page opens THEN it shows as Outros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(categoryId = "group-category-pet"))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(AppCategory.OTHER.key, state.category.id)
            assertNull(state.categoryParent)
        }

    @Test
    fun `GIVEN Bruno paid and three split it WHEN the page opens THEN shows Bruno as the payer and each part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("Bruno"), state.payers.map { it.name })
            assertEquals(listOf(ANA, BRUNO, CARLA), state.people.map { it.memberId })
            assertEquals(listOf(null, 64_200L, null), state.people.map { it.paidMinor })
            assertEquals(listOf(21_400L, 21_400L, 21_400L), state.people.map { it.shareMinor })
            assertEquals(3, state.participantCount)
        }

    @Test
    fun `GIVEN the receipt and two photos WHEN the page opens THEN shows the receipt first and numbers the photos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("receipt", "photo-1", "photo-2"), state.attachments.map { it.id })
            assertEquals(listOf(null, 1, 2), state.attachments.map { it.photoNumber })
            assertEquals(2, state.photoCount)
        }

    @Test
    fun `GIVEN a comment by Carla answered by Diego through the link WHEN the page opens THEN shows the thread`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("c1"), state.threads.map { it.comment.id })
            assertEquals(
                "Carla",
                state.threads
                    .single()
                    .comment.authorName,
            )
            assertEquals(
                listOf("Diego"),
                state.threads
                    .single()
                    .replies
                    .map { it.authorName },
            )
            assertTrue(
                state.threads
                    .single()
                    .replies
                    .single()
                    .viaLink,
            )
            assertEquals(2, state.commentCount)
            assertFalse(state.showsNoComments)
        }

    @Test
    fun `GIVEN a bill without comments WHEN the page opens THEN says there are none yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = emptyList()))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsNoComments)
        assertEquals(0, state.commentCount)
    }

    @Test
    fun `GIVEN a link turned off WHEN the page opens THEN says the link no longer works`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = SharedBill.NotFound)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SharedBillStatus.NOT_FOUND, state.status)
        assertFalse(state.showsSignIn)
        assertFalse(state.showsComposer)
    }

    @Test
    fun `GIVEN a link only the group opens WHEN the page opens signed out THEN says so and offers Entrar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = SharedBill.MembersOnly,
                accountStatus = AccountStatus.LocalOnly,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SharedBillStatus.MEMBERS_ONLY, state.status)
            assertTrue(state.showsSignIn)
            assertFalse(state.showsComposer)
        }

    @Test
    fun `GIVEN the server not answering yet WHEN the page opens THEN it is loading`() = runTest(testDispatcher) {
        // Given
        prepareScenario(loadGate = CompletableDeferred())

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SharedBillStatus.LOADING, state.status)
        assertFalse(state.showsSignIn)
        assertFalse(state.showsNoComments)
    }

    @Test
    fun `GIVEN the server fails WHEN the page opens THEN says it failed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBillError = IllegalStateException("server"))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SharedBillStatus.FAILED, state.status)
    }

    @Test
    fun `GIVEN the page failed WHEN tapping Tentar de novo THEN loads the bill again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBillError = IllegalStateException("server"))
        links.sharedBillError = null

        // When
        viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)

        // Then
        assertEquals(listOf(CODE, CODE), links.loadedCodes)
        assertEquals(SharedBillStatus.OPEN, viewModel.uiState.value.status)
    }

    @Test
    fun `GIVEN no internet WHEN the page opens THEN says it failed and asks the server nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SharedBillStatus.FAILED, state.status)
            assertFalse(state.isOnline)
            assertTrue(links.loadedCodes.isEmpty())
        }

    @Test
    fun `GIVEN the page failed offline WHEN the internet comes back THEN loads the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertEquals(listOf(CODE), links.loadedCodes)
        assertEquals(SharedBillStatus.OPEN, viewModel.uiState.value.status)
    }

    @Test
    fun `GIVEN the server failed WHEN the internet goes and comes back THEN loads the bill again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBillError = IllegalStateException("server"))
            links.sharedBillError = null
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertEquals(SharedBillStatus.OPEN, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the bill shown WHEN the internet goes and comes back THEN keeps it without asking again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            isOnline.value = false

            // When
            isOnline.value = true

            // Then
            assertEquals(listOf(CODE), links.loadedCodes)
            assertEquals(SharedBillStatus.OPEN, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN someone signed in WHEN the page opens THEN shows the composer and no Entrar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isSignedIn)
            assertTrue(state.showsComposer)
            assertFalse(state.showsSignIn)
            assertFalse(state.showsSignInToComment)
        }

    @Test
    fun `GIVEN an anonymous account WHEN the link is only for the group THEN offers Entrar for the group account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = SharedBill.MembersOnly)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isSignedIn)
            assertTrue(state.showsSignIn)
        }

    @Test
    fun `GIVEN a Google account outside the group WHEN the link is only for the group THEN offers no Entrar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = SharedBill.MembersOnly,
                accountStatus = AccountStatus.Connected("user-pierre", AccountProvider.GOOGLE),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SharedBillStatus.MEMBERS_ONLY, state.status)
            assertFalse(state.showsSignIn)
        }

    @Test
    fun `GIVEN nobody signed in WHEN the page opens THEN offers Entrar to comment instead of the composer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(accountStatus = AccountStatus.LocalOnly)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isSignedIn)
            assertFalse(state.showsComposer)
            assertTrue(state.showsSignIn)
            assertTrue(state.showsSignInToComment)
        }

    @Test
    fun `GIVEN a member with the bill on the device WHEN the page opens THEN offers Abrir no app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(isMember = true),
                storedExpense = expense,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.canOpenInApp)
        }

    @Test
    fun `GIVEN a member without the bill on the device WHEN the page opens THEN offers no Abrir no app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(isMember = true))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.canOpenInApp)
        }

    @Test
    fun `GIVEN a member without the bill on the device WHEN a sync brings it THEN offers Abrir no app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(isMember = true))

            // When
            expenses.expenses.value = listOf(expense)

            // Then
            assertTrue(viewModel.uiState.value.canOpenInApp)
        }

    @Test
    fun `GIVEN someone out of the group WHEN the page opens with the bill on the device THEN offers no Abrir no app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(isMember = false),
                storedExpense = expense,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.canOpenInApp)
        }

    @Test
    fun `GIVEN Abrir no app offered WHEN tapping it THEN opens the bill over the group Despesas`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(isMember = true),
                storedExpense = expense,
            )

            // When
            viewModel.onEvent(SharedBillUiEvent.OnOpenInAppClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.GroupDetail(
                        groupId = GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                    Route.BillDetail(
                        groupId = GROUP_ID,
                        expenseId = EXPENSE_ID,
                        focusCommentId = null,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a member without the bill on the device WHEN tapping Abrir no app THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(isMember = true))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnOpenInAppClicked)

            // Then
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no profile on the device WHEN tapping Entrar THEN opens the welcome alone`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            accountStatus = AccountStatus.LocalOnly,
            storedProfile = null,
        )

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSignInClicked)

        // Then
        assertEquals(listOf<Route>(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a profile on the device WHEN tapping Entrar THEN opens the profile over the page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(accountStatus = AccountStatus.LocalOnly)

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSignInClicked)

            // Then
            assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the page already closed WHEN tapping Entrar THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)
        navigator.navigateBack()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSignInClicked)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the page open WHEN closing it THEN returns to the home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the page open signed in WHEN typing a comment THEN keeps the draft and it can be sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // Then
            assertEquals("Valeu, Bruno!", viewModel.uiState.value.commentDraft)
            assertTrue(viewModel.uiState.value.canSendComment)
        }

    @Test
    fun `GIVEN the page open WHEN typing only spaces THEN the comment cannot be sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("   "))

        // Then
        assertFalse(viewModel.uiState.value.canSendComment)
    }

    @Test
    fun `GIVEN a comment typed WHEN the internet goes THEN it cannot be sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.canSendComment)
    }

    @Test
    fun `GIVEN nobody signed in WHEN typing a comment THEN it cannot be sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

        // Then
        assertFalse(viewModel.uiState.value.canSendComment)
    }

    @Test
    fun `GIVEN the comment of Carla WHEN replying to it THEN answers Carla in her thread`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))

        // Then
        assertEquals(
            BillReplyTarget(
                threadId = "c1",
                commentId = "c1",
                name = "Carla",
                isMe = false,
            ),
            viewModel.uiState.value.replyTarget,
        )
    }

    @Test
    fun `GIVEN Diego replied to Carla WHEN replying to Diego THEN answers Diego in the thread of Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("r1"))

            // Then
            assertEquals(
                BillReplyTarget(
                    threadId = "c1",
                    commentId = "r1",
                    name = "Diego",
                    isMe = false,
                ),
                viewModel.uiState.value.replyTarget,
            )
        }

    @Test
    fun `GIVEN replying to Carla WHEN cancelling the reply THEN the composer answers nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnReplyCancelled)

            // Then
            assertNull(viewModel.uiState.value.replyTarget)
        }

    @Test
    fun `GIVEN replying to Carla WHEN replying to a comment that is not there THEN still answers Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("gone"))

            // Then
            assertEquals(
                "c1",
                viewModel.uiState.value.replyTarget
                    ?.commentId,
            )
        }

    @Test
    fun `GIVEN a comment typed WHEN sending it THEN posts it with the profile name and loads the bill again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("  Valeu, Bruno!  "))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val posted = links.postedComments.single()
            assertEquals(CODE, posted.code)
            assertEquals("Valeu, Bruno!", posted.body)
            assertNull(posted.threadId)
            assertNull(posted.replyToName)
            assertEquals("Pierre", posted.authorName)
            assertEquals("", viewModel.uiState.value.commentDraft)
            assertFalse(viewModel.uiState.value.isSendingComment)
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
        }

    @Test
    fun `GIVEN no profile on the device WHEN sending a comment THEN posts it without a name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedProfile = null)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertNull(links.postedComments.single().authorName)
        }

    @Test
    fun `GIVEN replying to the reply of Diego WHEN sending THEN joins the thread of Carla and names Diego`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("r1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Fui eu"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val posted = links.postedComments.single()
            assertEquals("c1", posted.threadId)
            assertEquals("Diego", posted.replyToName)
            assertNull(viewModel.uiState.value.replyTarget)
        }

    @Test
    fun `GIVEN replying to the comment of Carla WHEN sending THEN joins her thread naming nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Fui eu"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val posted = links.postedComments.single()
            assertEquals("c1", posted.threadId)
            assertNull(posted.replyToName)
        }

    @Test
    fun `GIVEN only spaces typed WHEN sending the comment THEN nothing is posted`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("   "))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

        // Then
        assertTrue(links.postedComments.isEmpty())
    }

    @Test
    fun `GIVEN a comment on its way WHEN the server has not answered yet THEN it cannot be sent again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            links.commentGate = CompletableDeferred()
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isSendingComment)
            assertFalse(state.canSendComment)
            assertEquals("Valeu, Bruno!", state.commentDraft)
        }

    @Test
    fun `GIVEN a next comment typed while one goes up WHEN the first is posted THEN keeps the one typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val gate = CompletableDeferred<Unit>()
            links.commentGate = gate
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("E o gelo?"))

            // When
            gate.complete(Unit)

            // Then
            assertEquals("E o gelo?", viewModel.uiState.value.commentDraft)
        }

    @Test
    fun `GIVEN a member with the bill open WHEN the link is turned off THEN offers no Abrir no app`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(isMember = true),
                storedExpense = expense,
                commentOutcome = SharedBillComment.NOT_FOUND,
            )
            links.sharedBills.value = emptyMap()
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(SharedBillStatus.NOT_FOUND, viewModel.uiState.value.status)
            assertFalse(viewModel.uiState.value.canOpenInApp)
        }

    @Test
    fun `GIVEN the link turned off meanwhile WHEN sending a comment THEN the page says the link no longer works`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(commentOutcome = SharedBillComment.NOT_FOUND)
            links.sharedBills.value = emptyMap()
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertEquals(SharedBillStatus.NOT_FOUND, viewModel.uiState.value.status)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the link became only for the group WHEN sending a comment THEN the page says only the group opens it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(commentOutcome = SharedBillComment.MEMBERS_ONLY)
            links.sharedBills.value = mapOf(CODE to SharedBill.MembersOnly)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertEquals(SharedBillStatus.MEMBERS_ONLY, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the server knows no name WHEN sending a comment THEN asks for a profile and keeps the draft`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(commentOutcome = SharedBillComment.NEEDS_NAME)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowNeedsName), actions)
            assertEquals("Valeu, Bruno!", viewModel.uiState.value.commentDraft)
            assertFalse(viewModel.uiState.value.isSendingComment)
        }

    @Test
    fun `GIVEN the server fails WHEN sending a comment THEN says it failed and keeps the draft and the reply`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(commentError = IllegalStateException("server"))
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowCommentError), actions)
            assertEquals("Valeu, Bruno!", state.commentDraft)
            assertEquals("c1", state.replyTarget?.commentId)
            assertFalse(state.isSendingComment)
            assertTrue(state.canSendComment)
        }

    @Test
    fun `GIVEN a comment not posted WHEN sending it again THEN sends the same id so the server saves it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(commentOutcome = SharedBillComment.NEEDS_NAME)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val ids = links.postedComments.map { it.id }
            assertEquals(2, ids.size)
            assertEquals(ids[0], ids[1])
        }

    @Test
    fun `GIVEN a comment not posted WHEN changing its text and sending THEN sends it with a new id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(commentOutcome = SharedBillComment.NEEDS_NAME)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno! Já paguei"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            val ids = links.postedComments.map { it.id }
            assertEquals(2, ids.size)
            assertNotEquals(ids[0], ids[1])
        }

    @Test
    fun `GIVEN a comment posted WHEN sending another THEN the new one gets its own id`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

        // Then
        val ids = links.postedComments.map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `GIVEN the bill shown WHEN a comment goes up but the reload fails THEN keeps showing the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            links.sharedBillError = IllegalStateException("server")
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(SharedBillStatus.OPEN, viewModel.uiState.value.status)
            assertEquals("Jantar no Ostradamus", viewModel.uiState.value.description)
        }

    @Test
    fun `GIVEN the page open WHEN tapping the second photo THEN the viewer shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))

        // Then
        assertEquals("photo-2", viewModel.uiState.value.viewedAttachmentId)
        assertEquals(2, viewModel.uiState.value.viewedAttachmentIndex)
    }

    @Test
    fun `GIVEN the page open WHEN tapping a photo that is not there THEN the viewer stays closed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("gone"))

            // Then
            assertNull(viewModel.uiState.value.viewedAttachmentId)
        }

    @Test
    fun `GIVEN the viewer on the receipt WHEN paging to the first photo THEN shows that photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("receipt"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnViewerPageChanged("photo-1"))

            // Then
            assertEquals("photo-1", viewModel.uiState.value.viewedAttachmentId)
        }

    @Test
    fun `GIVEN the viewer open WHEN dismissing it THEN closes the viewer`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-1"))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnViewerDismissed)

        // Then
        assertNull(viewModel.uiState.value.viewedAttachmentId)
    }

    @Test
    fun `GIVEN the viewer on a photo removed meanwhile WHEN the bill loads again THEN closes the viewer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))
            links.sharedBills.value = mapOf(CODE to openBill(attachments = attachments.take(2)))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertNull(viewModel.uiState.value.viewedAttachmentId)
        }

    @Test
    fun `GIVEN the viewer on a photo WHEN the bill loads again with it THEN keeps showing it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals("photo-2", viewModel.uiState.value.viewedAttachmentId)
        }

    private val route = Route.SharedBill(code = CODE, commentId = null)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val earlyToday = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()
    private val yesterdayEvening = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

    private val pierre = UserProfile(
        displayName = "Pierre",
        avatarColor = AvatarColor.BLUE,
        photo = null,
    )

    private val attachments = listOf(
        attachment("receipt", BillAttachmentKind.RECEIPT),
        attachment("photo-1", BillAttachmentKind.PHOTO),
        attachment("photo-2", BillAttachmentKind.PHOTO),
    )

    // Why: Carla asked something and Diego, out of the group, answered her through the link.
    private val thread = listOf(
        comment(
            id = "c1",
            authorName = "Carla",
            threadId = null,
            viaLink = false,
        ),
        comment(
            id = "r1",
            authorName = "Diego",
            threadId = "c1",
            viaLink = true,
        ),
    )

    private val expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Jantar no Ostradamus",
        amountMinor = 64_200,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = BRUNO,
                amountMinor = 64_200,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = BRUNO,
                amountMinor = 64_200,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = yesterdayEvening,
        isPending = false,
        updatedAt = yesterdayEvening,
        updatedByMemberId = null,
    )

    private fun attachment(
        id: String,
        kind: BillAttachmentKind,
    ): SharedBillAttachment = SharedBillAttachment(
        id = id,
        kind = kind,
        width = 1600,
        height = 1200,
        addedByName = "Ana",
        addedAt = yesterdayEvening,
        imageUrl = "https://signed/$id.jpg",
    )

    private fun comment(
        id: String,
        authorName: String,
        threadId: String?,
        viaLink: Boolean,
    ): BillComment = BillComment(
        id = id,
        expenseId = EXPENSE_ID,
        groupId = GROUP_ID,
        authorUserId = null,
        authorMemberId = null,
        authorName = authorName,
        threadId = threadId,
        replyToName = null,
        body = "Quem trouxe o vinho?",
        viaLink = viaLink,
        postedAt = yesterdayEvening,
        isPending = false,
        editedAt = null,
        isDeleted = false,
        isMine = false,
        likes = emptyList(),
    )

    // Why: by default Bruno paid R$ 642 split equally among Ana, Bruno and Carla, seen by someone signed in.
    private fun openBill(
        isMember: Boolean = false,
        categoryId: String? = null,
        spentAt: Long = yesterdayEvening,
        attachments: List<SharedBillAttachment> = this.attachments,
        comments: List<BillComment> = thread,
    ): SharedBill.Open = SharedBill.Open(
        isMember = isMember,
        groupId = GROUP_ID,
        groupName = "Viagem Floripa",
        currency = GroupCurrency.USD,
        expenseId = EXPENSE_ID,
        description = "Jantar no Ostradamus",
        amountMinor = 64_200,
        spentAt = spentAt,
        categoryId = categoryId,
        splitMode = SplitMode.EQUAL,
        payers = listOf(SharedBillPart(BRUNO, "Bruno", 64_200)),
        shares = listOf(
            SharedBillPart(ANA, "Ana", 21_400),
            SharedBillPart(BRUNO, "Bruno", 21_400),
            SharedBillPart(CARLA, "Carla", 21_400),
        ),
        attachments = attachments,
        comments = comments,
    )

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        sharedBill: SharedBill = openBill(),
        sharedBillError: Throwable? = null,
        loadGate: CompletableDeferred<Unit>? = null,
        storedProfile: UserProfile? = pierre,
        storedExpense: Expense? = null,
        commentOutcome: SharedBillComment = SharedBillComment.POSTED,
        commentError: Throwable? = null,
        timeZone: TimeZone = TimeZone.UTC,
        accountStatus: AccountStatus = AccountStatus.Anonymous("user-pierre"),
    ) {
        isOnline.value = online
        links = FakeBillShareLinkRepository(initialSharedBills = mapOf(CODE to sharedBill)).also {
            it.sharedBillError = sharedBillError
            it.loadGate = loadGate
            it.commentOutcome = commentOutcome
            it.commentError = commentError
        }
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense))
        profiles = FakeProfileRepository(initialProfile = storedProfile)
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(route) }
        viewModel = SharedBillViewModel(
            route = route,
            navigator = navigator,
            loadSharedBill = LoadSharedBillUseCase(links),
            commentOnSharedBill = CommentOnSharedBillUseCase(links),
            editSharedBillComment = EditSharedBillCommentUseCase(links),
            deleteSharedBillComment = DeleteSharedBillCommentUseCase(links),
            likeSharedBillComment = LikeSharedBillCommentUseCase(links),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeProfile = ObserveProfileUseCase(profiles),
            observeAccountStatus = ObserveAccountStatusUseCase(FakeAccountRepository(accountStatus)),
            currentTimeMillis = { nowMillis },
            currentTimeZone = { timeZone },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
    }
}
