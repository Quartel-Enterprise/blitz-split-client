package com.quare.blitzsplit.feature.billdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
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
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentLiker
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentShare
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillStatus
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SharedBillViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
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
    fun `GIVEN the page failed while a read still hangs WHEN tapping Tentar de novo THEN reads the bill again`() =
        runTest(testDispatcher) {
            // Given
            val hangingRead = CompletableDeferred<Unit>()
            prepareScenario(loadGate = hangingRead)
            isOnline.value = false
            isOnline.value = true
            links.loadGate = null

            // When
            viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertEquals(SharedBillStatus.OPEN, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN a retry over a hanging read WHEN the old read answers THEN the page keeps the new one`() =
        runTest(testDispatcher) {
            // Given
            val hangingRead = CompletableDeferred<Unit>()
            prepareScenario(loadGate = hangingRead)
            isOnline.value = false
            isOnline.value = true
            links.loadGate = null
            viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)
            links.sharedBills.value = emptyMap()

            // When
            hangingRead.complete(Unit)

            // Then
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

    @Test
    fun `GIVEN a comment the caller wrote WHEN tapping Editar on it THEN its text goes in the composer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Eu levo"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))

            // Then
            val state = viewModel.uiState.value
            assertEquals("mine", state.editingCommentId)
            assertTrue(state.isEditingComment)
            assertEquals("Valeu, Bruno!", state.commentDraft)
            assertNull(state.replyTarget)
        }

    @Test
    fun `GIVEN the comment of Carla WHEN tapping Editar on it THEN nothing is edited`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("c1"))

        // Then
        assertNull(viewModel.uiState.value.editingCommentId)
        assertEquals("", viewModel.uiState.value.commentDraft)
    }

    @Test
    fun `GIVEN the page shown WHEN the internet goes THEN no comment can be changed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))
        assertTrue(viewModel.uiState.value.canChangeComments)

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.canChangeComments)
    }

    @Test
    fun `GIVEN nobody signed in WHEN the page opens THEN no comment can be changed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            sharedBill = openBill(comments = withMine),
            accountStatus = AccountStatus.LocalOnly,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.canChangeComments)
    }

    @Test
    fun `GIVEN a comment the caller wrote and no internet WHEN tapping Editar on it THEN nothing is edited`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            isOnline.value = false

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))

            // Then
            assertNull(viewModel.uiState.value.editingCommentId)
        }

    @Test
    fun `GIVEN editing after typing a reply WHEN cancelling the edit THEN gives back what was typed and the reply`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Eu levo"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditCancelled)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.editingCommentId)
            assertEquals("Eu levo", state.commentDraft)
            assertEquals("c1", state.replyTarget?.commentId)
        }

    @Test
    fun `GIVEN a comment of the caller edited WHEN sending THEN saves it reads the bill again and ends the edit`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Eu levo"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno! Até a próxima"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(listOf(Triple(CODE, "mine", "Valeu, Bruno! Até a próxima")), links.editedComments)
            assertTrue(links.postedComments.isEmpty())
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            val state = viewModel.uiState.value
            assertNull(state.editingCommentId)
            assertEquals("Eu levo", state.commentDraft)
            assertFalse(state.isChangingComment)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the link became only for the group WHEN saving an edit THEN reads the page again and the edit stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(comments = withMine),
                changeOutcome = SharedBillComment.MEMBERS_ONLY,
            )
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno! Até a próxima"))
            links.sharedBills.value = mapOf(CODE to SharedBill.MembersOnly)

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            val state = viewModel.uiState.value
            assertEquals(SharedBillStatus.MEMBERS_ONLY, state.status)
            assertEquals("mine", state.editingCommentId)
            assertEquals("Valeu, Bruno! Até a próxima", state.commentDraft)
        }

    @Test
    fun `GIVEN the server does not answer WHEN saving an edit THEN says it failed keeps the edit and reads nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(comments = withMine),
                changeError = IllegalStateException("offline"),
            )
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno! Até a próxima"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowCommentChangeError), actions)
            assertEquals(listOf(CODE), links.loadedCodes)
            val state = viewModel.uiState.value
            assertEquals("mine", state.editingCommentId)
            assertEquals("Valeu, Bruno! Até a próxima", state.commentDraft)
            assertFalse(state.isChangingComment)
        }

    @Test
    fun `GIVEN editing a comment WHEN the page reads it again deleted THEN the edit ends`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Eu levo"))
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
        links.sharedBills.value = mapOf(CODE to openBill(comments = thread))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.editingCommentId)
        assertEquals("Eu levo", state.commentDraft)
    }

    @Test
    fun `GIVEN a comment the caller wrote WHEN tapping Apagar on it THEN asks first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

        // Then
        assertEquals("mine", viewModel.uiState.value.commentToDelete)
        assertTrue(links.deletedComments.isEmpty())
    }

    @Test
    fun `GIVEN the comment of Carla WHEN tapping Apagar on it THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("c1"))

        // Then
        assertNull(viewModel.uiState.value.commentToDelete)
    }

    @Test
    fun `GIVEN Apagar comentario asks WHEN dismissing THEN closes the question`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteDismissed)

        // Then
        assertNull(viewModel.uiState.value.commentToDelete)
        assertTrue(links.deletedComments.isEmpty())
    }

    @Test
    fun `GIVEN Apagar comentario asks WHEN confirming THEN deletes it on the server and reads the bill again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)

            // Then
            assertEquals(listOf(CODE to "mine"), links.deletedComments)
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertNull(viewModel.uiState.value.commentToDelete)
        }

    @Test
    fun `GIVEN Apagar comentario asks and the internet gone WHEN confirming THEN closes it and says it failed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))
            isOnline.value = false
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)

            // Then
            assertNull(viewModel.uiState.value.commentToDelete)
            assertTrue(links.deletedComments.isEmpty())
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowCommentChangeError), actions)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN editing the comment Apagar asks about WHEN confirming THEN the edit ends too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Eu levo"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.editingCommentId)
            assertEquals("Eu levo", state.commentDraft)
        }

    @Test
    fun `GIVEN the server does not answer WHEN deleting a comment THEN says it failed and reads nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(comments = withMine),
                changeError = IllegalStateException("offline"),
            )
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)

            // Then
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowCommentChangeError), actions)
            assertEquals(listOf(CODE), links.loadedCodes)
        }

    @Test
    fun `GIVEN the comment of Carla WHEN tapping its heart THEN likes it on the server and reads the bill again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertEquals(listOf(Triple(CODE, "c1", true)), links.likedComments)
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertFalse(viewModel.uiState.value.isChangingComment)
        }

    @Test
    fun `GIVEN a comment the caller liked WHEN tapping its heart THEN undoes the like`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

        // Then
        assertEquals(listOf(Triple(CODE, "c1", false)), links.likedComments)
    }

    @Test
    fun `GIVEN a deleted comment kept over a reply WHEN tapping its heart THEN nothing is liked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = deletedOverReply))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertTrue(links.likedComments.isEmpty())
            assertEquals(listOf(CODE), links.loadedCodes)
        }

    @Test
    fun `GIVEN the page shown and the internet gone WHEN tapping a heart THEN nothing is liked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            isOnline.value = false

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertTrue(links.likedComments.isEmpty())
        }

    @Test
    fun `GIVEN the link turned off meanwhile WHEN tapping a heart THEN the page says the link no longer works`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(changeOutcome = SharedBillComment.NOT_FOUND)
            links.sharedBills.value = emptyMap()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertEquals(SharedBillStatus.NOT_FOUND, viewModel.uiState.value.status)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a like on its way WHEN the server has not answered yet THEN no other change can go`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(changeGate = CompletableDeferred())

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isChangingComment)
            assertFalse(state.canChangeComments)
        }

    @Test
    fun `GIVEN a like on its way WHEN tapping another heart before it answers THEN only the first goes`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(changeGate = gate)
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("r1"))

            // When
            gate.complete(Unit)

            // Then
            assertEquals(listOf(Triple(CODE, "c1", true)), links.likedComments)
            assertFalse(viewModel.uiState.value.isChangingComment)
        }

    @Test
    fun `GIVEN a comment liked by Ana and the caller WHEN tapping its count THEN lists who liked it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))

            // Then
            assertEquals(
                listOf(
                    BillCommentLiker(
                        name = "Ana",
                        isMe = false,
                    ),
                    BillCommentLiker(
                        name = "Pierre",
                        isMe = true,
                    ),
                ),
                viewModel.uiState.value.likesComment
                    ?.likers,
            )
        }

    @Test
    fun `GIVEN a comment nobody liked WHEN tapping its count THEN Curtidas stays closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))

        // Then
        assertNull(viewModel.uiState.value.likesCommentId)
    }

    @Test
    fun `GIVEN Curtidas open WHEN dismissing it THEN closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesDismissed)

        // Then
        assertNull(viewModel.uiState.value.likesComment)
    }

    @Test
    fun `GIVEN Curtidas open WHEN the page reads the comment again without likes THEN it closes by itself`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))
            links.sharedBills.value = mapOf(CODE to openBill(comments = listOf(likedByMe.copy(likes = emptyList()))))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertNull(viewModel.uiState.value.likesComment)
        }

    @Test
    fun `GIVEN the comment of Carla WHEN sharing it THEN hands the page link at it with its text and the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentShareClicked("c1"))

            // Then
            assertEquals(
                listOf<SharedBillUiAction>(
                    SharedBillUiAction.ShareComment(
                        BillCommentShare(
                            link = "https://blitzsplit.com/c/$CODE?comentario=c1",
                            authorName = "Carla",
                            isMe = false,
                            billDescription = "Jantar no Ostradamus",
                            body = "Quem trouxe o vinho?",
                        ),
                    ),
                ),
                actions,
            )
        }

    @Test
    fun `GIVEN a deleted comment kept over a reply WHEN sharing it THEN nothing is shared`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = deletedOverReply))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentShareClicked("c1"))

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN a deleted comment kept over a reply WHEN the page opens THEN counts only the reply`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = deletedOverReply))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(1, state.commentCount)
            assertTrue(
                state.threads
                    .single()
                    .comment.isDeleted,
            )
        }

    @Test
    fun `GIVEN a link shared at the reply of Diego WHEN the bill shows THEN highlights it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(focusCommentId = "r1")

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("r1", state.focusedCommentId)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN a link shared at a comment no longer there WHEN the bill shows THEN says it was deleted`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                loadGate = gate,
                focusCommentId = "gone",
            )

            // When
            gate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.focusedCommentId)
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowCommentGone), actions)
        }

    @Test
    fun `GIVEN a link shared at a comment opened offline WHEN the internet comes back THEN highlights it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                online = false,
                focusCommentId = "c1",
            )
            assertTrue(actions.isEmpty())

            // When
            isOnline.value = true

            // Then
            assertEquals("c1", viewModel.uiState.value.focusedCommentId)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the highlight shown WHEN the page reads the bill again THEN nothing is highlighted again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(focusCommentId = "c1")
            viewModel.onEvent(SharedBillUiEvent.OnFocusedCommentShown)

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertEquals(listOf(CODE, CODE), links.loadedCodes)
            assertNull(viewModel.uiState.value.focusedCommentId)
        }

    @Test
    fun `GIVEN a link shared at a comment gone WHEN the page reads the bill again THEN says so only once`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(
                loadGate = gate,
                focusCommentId = "gone",
            )
            gate.complete(Unit)

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertEquals(listOf<SharedBillUiAction>(SharedBillUiAction.ShowCommentGone), actions)
        }

    @Test
    fun `GIVEN an open link WHEN the page opens THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SharedBillStatus.OPEN, state.status)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the page failed WHEN tapping Tentar de novo THEN sends the retry clicked from the shared page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBillError = IllegalStateException("server"))
            links.sharedBillError = null

            // When
            viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf("retry_clicked" to mapOf<String, Any>("source" to "shared_bill")), trackedEvents)
        }

    @Test
    fun `GIVEN the page failed while a read still hangs WHEN tapping Tentar de novo THEN sends the retry clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(loadGate = CompletableDeferred())
            isOnline.value = false
            isOnline.value = true
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf("retry_clicked" to mapOf<String, Any>("source" to "shared_bill")), trackedEvents)
        }

    @Test
    fun `GIVEN the bill being read WHEN tapping Tentar de novo THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(loadGate = CompletableDeferred())

        // When
        viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
        assertEquals(listOf(CODE), links.loadedCodes)
    }

    @Test
    fun `GIVEN a profile on the device WHEN tapping Entrar THEN sends the sign in clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSignInClicked)

        // Then
        assertEquals(listOf("shared_bill_sign_in_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the page already closed WHEN tapping Entrar THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)
        navigator.navigateBack()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSignInClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Abrir no app offered WHEN tapping it THEN sends the open in app clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            sharedBill = openBill(isMember = true),
            storedExpense = expense,
        )

        // When
        viewModel.onEvent(SharedBillUiEvent.OnOpenInAppClicked)

        // Then
        assertEquals(listOf("shared_bill_open_in_app_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a member without the bill on the device WHEN tapping Abrir no app THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(isMember = true))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnOpenInAppClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the profile open over the page WHEN a late Abrir no app arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sharedBill = openBill(isMember = true),
                storedExpense = expense,
            )
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(SharedBillUiEvent.OnOpenInAppClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the page open WHEN closing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCloseClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the page open WHEN tapping the second photo THEN sends the attachment clicked from the shared page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))

            // Then
            assertEquals(
                listOf("bill_attachment_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the page open WHEN tapping a photo that is not there THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("gone"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the viewer on the receipt WHEN paging to the first photo THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("receipt"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnViewerPageChanged("photo-1"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the viewer open WHEN dismissing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-1"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnViewerDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the viewer on the second photo WHEN tapping Download THEN sends it from the shared page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)

            // Then
            assertEquals(
                listOf("bill_attachment_download_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the viewer paged to the first photo WHEN tapping Download THEN sends the download clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("receipt"))
            viewModel.onEvent(SharedBillUiEvent.OnViewerPageChanged("photo-1"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)

            // Then
            assertEquals(
                listOf("bill_attachment_download_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the viewer on a photo WHEN tapping Download THEN nothing changes and no action is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))
            actions.clear()
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertEquals("photo-2", viewModel.uiState.value.viewedAttachmentId)
            assertTrue(actions.isEmpty())
            assertEquals(listOf(Route.Home, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the page open and the viewer never opened WHEN tapping Download THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the viewer dismissed WHEN a late Download tap arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-1"))
        viewModel.onEvent(SharedBillUiEvent.OnViewerDismissed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the viewer closed as its photo was removed WHEN a late Download tap arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked("photo-2"))
            links.sharedBills.value = mapOf(CODE to openBill(attachments = attachments.take(2)))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a comment typed WHEN sending it THEN sends the comment send clicked from the shared page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(
                listOf(
                    "comment_send_clicked" to mapOf<String, Any>(
                        "is_reply" to false,
                        "is_edit" to false,
                        "source" to "shared_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN replying to Carla WHEN sending THEN the comment send clicked says it is a reply`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Eu trouxe"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(
                listOf(
                    "comment_send_clicked" to mapOf<String, Any>(
                        "is_reply" to true,
                        "is_edit" to false,
                        "source" to "shared_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a comment of the caller edited WHEN sending THEN the comment send clicked says it is an edit`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno e Ana!"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(
                listOf(
                    "comment_send_clicked" to mapOf<String, Any>(
                        "is_reply" to false,
                        "is_edit" to true,
                        "source" to "shared_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN editing and then replying to Carla WHEN saving THEN the comment send clicked is not a reply`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
            viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno e Ana!"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

            // Then
            assertEquals(
                listOf(
                    "comment_send_clicked" to mapOf<String, Any>(
                        "is_reply" to false,
                        "is_edit" to true,
                        "source" to "shared_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a comment typed WHEN the internet goes and sending it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
        isOnline.value = false

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN nobody signed in WHEN sending a comment THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a comment on its way WHEN sending it again THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        links.commentGate = CompletableDeferred()
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the page open WHEN typing a comment THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged("Valeu, Bruno!"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the comment of Carla WHEN replying to it THEN sends the comment reply clicked from the shared page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))

            // Then
            assertEquals(
                listOf("comment_reply_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the page open WHEN replying to a comment that is not there THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("gone"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN replying to Carla WHEN cancelling the reply THEN sends the reply cancelled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SharedBillUiEvent.OnReplyClicked("c1"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnReplyCancelled)

        // Then
        assertEquals(listOf("comment_reply_cancelled" to mapOf<String, Any>("source" to "shared_bill")), trackedEvents)
    }

    @Test
    fun `GIVEN replying to nobody WHEN cancelling the reply THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnReplyCancelled)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a comment the caller wrote WHEN tapping Editar on it THEN sends the comment edit clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))

            // Then
            assertEquals(
                listOf("comment_edit_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the comment of Carla WHEN tapping Editar on it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("c1"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a comment the caller wrote and no internet WHEN tapping Editar on it THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            isOnline.value = false

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN editing a comment WHEN cancelling the edit THEN sends the edit cancelled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked("mine"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditCancelled)

        // Then
        assertEquals(listOf("comment_edit_cancelled" to mapOf<String, Any>("source" to "shared_bill")), trackedEvents)
    }

    @Test
    fun `GIVEN no comment being edited WHEN cancelling the edit THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditCancelled)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a comment the caller wrote WHEN tapping Apagar on it THEN sends the comment delete clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

            // Then
            assertEquals(
                listOf("comment_delete_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the comment of Carla WHEN tapping Apagar on it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("c1"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a comment the caller wrote and no internet WHEN tapping Apagar on it THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            isOnline.value = false

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Apagar comentario asks WHEN confirming THEN sends the comment delete confirmed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)

            // Then
            assertEquals(
                listOf("comment_delete_confirmed" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Apagar comentario never asked WHEN a confirmation arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Apagar comentario asks WHEN dismissing THEN sends the comment delete cancelled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = withMine))
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked("mine"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteDismissed)

            // Then
            assertEquals(
                listOf("comment_delete_cancelled" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Apagar comentario never asked WHEN a dismiss arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = withMine))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the comment of Carla WHEN tapping its heart THEN sends the like toggled as liked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertEquals(
                listOf(
                    "comment_like_toggled" to mapOf<String, Any>(
                        "is_liked" to true,
                        "source" to "shared_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a comment the caller liked WHEN tapping its heart THEN sends the like toggled as not liked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertEquals(
                listOf(
                    "comment_like_toggled" to mapOf<String, Any>(
                        "is_liked" to false,
                        "source" to "shared_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a deleted comment kept over a reply WHEN tapping its heart THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = deletedOverReply))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the page shown and the internet gone WHEN tapping a heart THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            isOnline.value = false

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN nobody signed in WHEN tapping a heart THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(accountStatus = AccountStatus.LocalOnly)

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a like on its way WHEN tapping another heart before it answers THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(changeGate = CompletableDeferred())
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("c1"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked("r1"))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a comment liked by Ana and the caller WHEN tapping its count THEN sends the likes clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))

            // Then
            assertEquals(
                listOf("comment_likes_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a comment nobody liked WHEN tapping its count THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Curtidas open WHEN dismissing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = listOf(likedByMe)))
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked("c1"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the comment of Carla WHEN sharing it THEN sends the comment share clicked from the shared page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SharedBillUiEvent.OnCommentShareClicked("c1"))

            // Then
            assertEquals(
                listOf("comment_share_clicked" to mapOf<String, Any>("source" to "shared_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a deleted comment kept over a reply WHEN sharing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sharedBill = openBill(comments = deletedOverReply))

        // When
        viewModel.onEvent(SharedBillUiEvent.OnCommentShareClicked("c1"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a link shared at the reply of Diego WHEN it was shown THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(focusCommentId = "r1")

        // When
        viewModel.onEvent(SharedBillUiEvent.OnFocusedCommentShown)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    // Why: the comment the link was shared at, set by each scenario.
    private var route = Route.SharedBill(
        code = CODE,
        commentId = null,
    )

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

    // Why: what Carla and Diego wrote, then the caller's own comment, which the server says is theirs.
    private val withMine = thread + comment(
        id = "mine",
        authorName = "Pierre",
        threadId = null,
        viaLink = true,
        body = "Valeu, Bruno!",
        isMine = true,
    )

    // Why: Carla deleted what she asked, which stays as "Comentário apagado" over Diego's answer.
    private val deletedOverReply = listOf(
        comment(
            id = "c1",
            authorName = "Carla",
            threadId = null,
            viaLink = false,
            body = "",
            isDeleted = true,
        ),
        comment(
            id = "r1",
            authorName = "Diego",
            threadId = "c1",
            viaLink = true,
        ),
    )

    private val likedByMe = comment(
        id = "c1",
        authorName = "Carla",
        threadId = null,
        viaLink = false,
        likes = listOf(
            BillCommentLike(
                memberId = null,
                name = "Ana",
                isMine = false,
            ),
            BillCommentLike(
                memberId = null,
                name = "Pierre",
                isMine = true,
            ),
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
        body: String = "Quem trouxe o vinho?",
        isMine: Boolean = false,
        isDeleted: Boolean = false,
        likes: List<BillCommentLike> = emptyList(),
    ): BillComment = BillComment(
        id = id,
        expenseId = EXPENSE_ID,
        groupId = GROUP_ID,
        authorUserId = null,
        authorMemberId = null,
        authorName = authorName,
        threadId = threadId,
        replyToName = null,
        body = body,
        viaLink = viaLink,
        postedAt = yesterdayEvening,
        isPending = false,
        editedAt = null,
        isDeleted = isDeleted,
        isMine = isMine,
        likes = likes,
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
        changeOutcome: SharedBillComment = SharedBillComment.POSTED,
        changeError: Throwable? = null,
        changeGate: CompletableDeferred<Unit>? = null,
        focusCommentId: String? = null,
    ) {
        route = route.copy(commentId = focusCommentId)
        isOnline.value = online
        links = FakeBillShareLinkRepository(initialSharedBills = mapOf(CODE to sharedBill)).also {
            it.sharedBillError = sharedBillError
            it.loadGate = loadGate
            it.commentOutcome = commentOutcome
            it.commentError = commentError
            it.changeOutcome = changeOutcome
            it.changeError = changeError
            it.changeGate = changeGate
        }
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense))
        profiles = FakeProfileRepository(initialProfile = storedProfile)
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(route) }
        viewModel = SharedBillViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
