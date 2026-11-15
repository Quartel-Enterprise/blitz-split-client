package com.quare.blitzsplit.core.support.domain

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.model.ReviewPromptState
import com.quare.blitzsplit.core.support.domain.repository.ReviewPromptRepository
import com.quare.blitzsplit.core.support.domain.usecase.impl.RequestReviewIfNeededUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestReviewIfNeededUseCaseTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private lateinit var repository: FakeReviewPromptRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var useCase: RequestReviewIfNeededUseCase

    @Test
    fun `GIVEN no bill yet WHEN the first bill is saved THEN does not ask and counts it`() = runTest {
        // Given
        prepareScenario()

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertEquals(state(billsCreated = 1), repository.state)
    }

    @Test
    fun `GIVEN one bill WHEN the second bill is saved THEN does not ask nor start the days of grace`() = runTest {
        // Given
        prepareScenario(stored = state(billsCreated = 1))

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertEquals(state(billsCreated = 2), repository.state)
    }

    @Test
    fun `GIVEN two bills WHEN the third bill is saved THEN only starts the days of grace`() = runTest {
        // Given
        prepareScenario(stored = state(billsCreated = 2))

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertEquals(
            state(
                billsCreated = 3,
                firstEligibleAt = NOW,
            ),
            repository.state,
        )
    }

    @Test
    fun `GIVEN the grace started almost 3 days ago WHEN a bill is saved THEN does not ask but counts it`() = runTest {
        // Given
        val stored = state(
            billsCreated = 3,
            firstEligibleAt = NOW - 3 * DAY + 1,
        )
        prepareScenario(stored = stored)

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertEquals(stored.copy(billsCreated = 4), repository.state)
    }

    @Test
    fun `GIVEN the grace started 3 days ago WHEN a bill is saved THEN asks and remembers when and on which version`() =
        runTest {
            // Given
            val stored = state(
                billsCreated = 3,
                firstEligibleAt = NOW - 3 * DAY,
            )
            prepareScenario(stored = stored)

            // When
            val asks = useCase(ReviewMoment.BillCreated)

            // Then
            assertTrue(asks)
            assertEquals(
                stored.copy(
                    billsCreated = 4,
                    lastPromptedAt = NOW,
                    lastPromptedVersion = APP_VERSION,
                ),
                repository.state,
            )
        }

    @Test
    fun `GIVEN this version asked long ago WHEN a bill is saved THEN does not ask again`() = runTest {
        // Given
        val stored = state(
            billsCreated = 9,
            firstEligibleAt = NOW - 200 * DAY,
            lastPromptedAt = NOW - 100 * DAY,
            lastPromptedVersion = APP_VERSION,
        )
        prepareScenario(stored = stored)

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertEquals(stored.copy(billsCreated = 10), repository.state)
    }

    @Test
    fun `GIVEN an earlier version asked 60 days ago WHEN a bill is saved THEN asks on the new version`() = runTest {
        // Given
        val stored = state(
            billsCreated = 9,
            firstEligibleAt = NOW - 200 * DAY,
            lastPromptedAt = NOW - 60 * DAY,
            lastPromptedVersion = EARLIER_VERSION,
        )
        prepareScenario(stored = stored)

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
        assertEquals(
            stored.copy(
                billsCreated = 10,
                lastPromptedAt = NOW,
                lastPromptedVersion = APP_VERSION,
            ),
            repository.state,
        )
    }

    @Test
    fun `GIVEN an earlier version asked almost 60 days ago WHEN a bill is saved THEN does not ask but counts it`() =
        runTest {
            // Given
            val stored = state(
                billsCreated = 9,
                firstEligibleAt = NOW - 200 * DAY,
                lastPromptedAt = NOW - 60 * DAY + 1,
                lastPromptedVersion = EARLIER_VERSION,
            )
            prepareScenario(stored = stored)

            // When
            val asks = useCase(ReviewMoment.BillCreated)

            // Then
            assertFalse(asks)
            assertEquals(stored.copy(billsCreated = 10), repository.state)
        }

    @Test
    fun `GIVEN the clock was set back to before the grace started WHEN a bill is saved THEN asks`() = runTest {
        // Given
        prepareScenario(
            stored = state(
                billsCreated = 3,
                firstEligibleAt = NOW + 400 * DAY,
            ),
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN the clock was set back to before an earlier version asked WHEN a bill is saved THEN asks`() = runTest {
        // Given
        prepareScenario(
            stored = state(
                billsCreated = 9,
                firstEligibleAt = NOW - 200 * DAY,
                lastPromptedAt = NOW + 400 * DAY,
                lastPromptedVersion = EARLIER_VERSION,
            ),
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
        assertEquals(NOW, repository.state.lastPromptedAt)
    }

    @Test
    fun `GIVEN the web past every gate WHEN a bill is saved THEN never asks`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            stored = pastGrace,
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertNull(repository.state.lastPromptedAt)
        assertNull(repository.state.lastPromptedVersion)
    }

    @Test
    fun `GIVEN the web past every gate WHEN a payment settles a group THEN never asks`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.WEB,
            stored = pastGrace,
            balances = mapOf(TRIP to settled),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

        // Then
        assertFalse(asks)
        assertEquals(pastGrace, repository.state)
    }

    @Test
    fun `GIVEN the iPhone app past the grace WHEN a bill is saved THEN asks`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.IOS,
            stored = pastGrace,
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN the Mac app past the grace WHEN a bill is saved THEN asks`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.MACOS,
            stored = pastGrace,
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN nothing remembered WHEN a payment settles a group THEN only starts the days of grace`() = runTest {
        // Given
        prepareScenario(balances = mapOf(TRIP to settled))

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

        // Then
        assertFalse(asks)
        assertEquals(state(firstEligibleAt = NOW), repository.state)
    }

    @Test
    fun `GIVEN past the grace WHEN a payment settles a group THEN asks without counting a bill`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            balances = mapOf(TRIP to settled),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

        // Then
        assertTrue(asks)
        assertEquals(
            pastGrace.copy(
                lastPromptedAt = NOW,
                lastPromptedVersion = APP_VERSION,
            ),
            repository.state,
        )
    }

    @Test
    fun `GIVEN nothing remembered WHEN a payment leaves debts in the group THEN does not start the days of grace`() =
        runTest {
            // Given
            prepareScenario(balances = mapOf(TRIP to owing))

            // When
            val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

            // Then
            assertFalse(asks)
            assertEquals(state(), repository.state)
        }

    @Test
    fun `GIVEN past the grace WHEN a payment leaves debts in the group THEN does not ask`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            balances = mapOf(TRIP to owing),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

        // Then
        assertFalse(asks)
        assertEquals(pastGrace, repository.state)
    }

    @Test
    fun `GIVEN past the grace WHEN a payment is recorded in a group with no bills THEN does not ask`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            balances = mapOf(TRIP to listOf(balancesIn(GroupCurrency.BRL, hasBills = false))),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

        // Then
        assertFalse(asks)
        assertEquals(pastGrace, repository.state)
    }

    @Test
    fun `GIVEN past the grace WHEN a payment is recorded in a group with no balances at all THEN does not ask`() =
        runTest {
            // Given
            prepareScenario(
                stored = pastGrace,
                balances = mapOf(TRIP to emptyList()),
            )

            // When
            val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

            // Then
            assertFalse(asks)
        }

    @Test
    fun `GIVEN past the grace WHEN a payment settles one currency and another still has a debt THEN does not ask`() =
        runTest {
            // Given
            val oneCurrencyOpen = listOf(
                balancesIn(GroupCurrency.BRL),
                balancesIn(GroupCurrency.USD, debts = listOf(Debt("bruno", "ana", 1_500))),
            )
            prepareScenario(
                stored = pastGrace,
                balances = mapOf(TRIP to oneCurrencyOpen),
            )

            // When
            val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

            // Then
            assertFalse(asks)
        }

    @Test
    fun `GIVEN past the grace WHEN a payment settles every currency of the group THEN asks`() = runTest {
        // Given
        val everyCurrencySettled = listOf(
            balancesIn(GroupCurrency.BRL),
            balancesIn(GroupCurrency.USD),
        )
        prepareScenario(
            stored = pastGrace,
            balances = mapOf(TRIP to everyCurrencySettled),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP)))

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN past the grace WHEN payments settle only the last of two groups THEN asks`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            balances = mapOf(
                TRIP to owing,
                HOUSE to settled,
            ),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP, HOUSE)))

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN past the grace WHEN payments leave debts in both groups THEN does not ask`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            balances = mapOf(
                TRIP to owing,
                HOUSE to owing,
            ),
        )

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(listOf(TRIP, HOUSE)))

        // Then
        assertFalse(asks)
    }

    @Test
    fun `GIVEN past the grace WHEN a payment names no group THEN does not ask`() = runTest {
        // Given
        prepareScenario(stored = pastGrace)

        // When
        val asks = useCase(ReviewMoment.PaymentRecorded(emptyList()))

        // Then
        assertFalse(asks)
    }

    @Test
    fun `GIVEN a group screen on top WHEN a bill is saved THEN asks`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            screensOverHome = listOf(Route.GroupDetail.expensesOf(TRIP)),
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN Activity on top WHEN a bill is saved THEN asks`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            screensOverHome = listOf(Route.Activity.all),
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertTrue(asks)
    }

    @Test
    fun `GIVEN a form on top WHEN it closes within 10 seconds THEN asks`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            screensOverHome = listOf(Route.CreateGroup),
        )
        val asks = async { useCase(ReviewMoment.BillCreated) }
        advanceTimeBy(ALMOST_10_SECONDS)
        runCurrent()

        // When
        navigator.navigateBack()

        // Then
        assertTrue(asks.await())
        assertEquals(NOW, repository.state.lastPromptedAt)
        assertEquals(APP_VERSION, repository.state.lastPromptedVersion)
    }

    @Test
    fun `GIVEN a form on top WHEN it stays open for 10 seconds THEN gives up without remembering it asked`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            screensOverHome = listOf(Route.CreateGroup),
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertEquals(pastGrace.copy(billsCreated = pastGrace.billsCreated + 1), repository.state)
    }

    @Test
    fun `GIVEN the form closes only after the 10 seconds WHEN it closes THEN the answer was already no`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            screensOverHome = listOf(Route.CreateGroup),
        )
        val asks = async { useCase(ReviewMoment.BillCreated) }
        advanceTimeBy(JUST_OVER_10_SECONDS)
        runCurrent()

        // When
        navigator.navigateBack()

        // Then
        assertFalse(asks.await())
        assertNull(repository.state.lastPromptedAt)
    }

    @Test
    fun `GIVEN the group opened over a bill form on top WHEN a bill is saved THEN does not ask over it`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            screensOverHome = listOf(Route.GroupDetail.expensesOf(TRIP).copy(isOverBill = true)),
        )

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
        assertNull(repository.state.lastPromptedAt)
    }

    @Test
    fun `GIVEN a moment gave up waiting for a form to close WHEN the next bill is saved over a home tab THEN asks`() =
        runTest {
            // Given
            prepareScenario(
                stored = pastGrace,
                screensOverHome = listOf(Route.CreateGroup),
            )
            useCase(ReviewMoment.BillCreated)
            navigator.navigateBack()

            // When
            val asks = useCase(ReviewMoment.BillCreated)

            // Then
            assertTrue(asks)
        }

    @Test
    fun `GIVEN it just asked WHEN another bill is saved on the same version THEN does not ask twice`() = runTest {
        // Given
        prepareScenario(stored = pastGrace)
        useCase(ReviewMoment.BillCreated)

        // When
        val asks = useCase(ReviewMoment.BillCreated)

        // Then
        assertFalse(asks)
    }

    @Test
    fun `GIVEN the grace is over WHEN a bill is saved THEN sends the review requested by a saved bill`() = runTest {
        // Given
        prepareScenario(stored = pastGrace)

        // When
        useCase(ReviewMoment.BillCreated)

        // Then
        assertEquals(listOf("in_app_review_requested" to mapOf<String, Any>("trigger" to "bill_saved")), trackedEvents)
    }

    @Test
    fun `GIVEN the grace is over WHEN a payment settles a group THEN sends the review requested by it`() = runTest {
        // Given
        prepareScenario(
            stored = pastGrace,
            balances = mapOf("group-trip" to settled),
        )

        // When
        useCase(ReviewMoment.PaymentRecorded(listOf("group-trip")))

        // Then
        assertEquals(
            listOf("in_app_review_requested" to mapOf<String, Any>("trigger" to "group_settled")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the grace only starts WHEN a bill is saved THEN sends no event`() = runTest {
        // Given
        prepareScenario(stored = state(billsCreated = 2))

        // When
        useCase(ReviewMoment.BillCreated)

        // Then
        assertEquals(emptyList(), trackedEvents)
    }

    private val pastGrace = state(
        billsCreated = 5,
        firstEligibleAt = NOW - 3 * DAY,
    )

    private val settled = listOf(balancesIn(GroupCurrency.BRL))

    private val owing = listOf(balancesIn(GroupCurrency.BRL, debts = listOf(Debt("bruno", "ana", 4_200))))

    private fun state(
        billsCreated: Int = 0,
        firstEligibleAt: Long? = null,
        lastPromptedAt: Long? = null,
        lastPromptedVersion: String? = null,
    ): ReviewPromptState = ReviewPromptState(
        billsCreated = billsCreated,
        firstEligibleAt = firstEligibleAt,
        lastPromptedAt = lastPromptedAt,
        lastPromptedVersion = lastPromptedVersion,
    )

    private fun balancesIn(
        currency: GroupCurrency,
        hasBills: Boolean = true,
        debts: List<Debt> = emptyList(),
    ): CurrencyBalances = CurrencyBalances(
        currency = currency,
        balances = GroupBalances(
            debts = debts,
            hasBills = hasBills,
            unsimplifiedDebts = null,
        ),
    )

    private fun prepareScenario(
        platform: ClientPlatform = ClientPlatform.ANDROID,
        stored: ReviewPromptState = state(),
        balances: Map<String, List<CurrencyBalances>> = emptyMap(),
        screensOverHome: List<Route> = emptyList(),
    ) {
        repository = FakeReviewPromptRepository(stored)
        navigator = BackStackNavigator(startRoute = Route.Home).also { screensOverHome.forEach(it::navigate) }
        useCase = RequestReviewIfNeededUseCase(
            appVersion = APP_VERSION,
            platform = platform,
            navigator = navigator,
            repository = repository,
            observeGroupBalances = { groupId -> flowOf(balances.getValue(groupId)) },
            currentTimeMillis = { NOW },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
    }

    private companion object {
        const val APP_VERSION = "3.1.0"
        const val EARLIER_VERSION = "3.0.0"
        const val NOW = 1_800_000_000_000L
        const val DAY = 86_400_000L
        const val ALMOST_10_SECONDS = 9_999L
        const val JUST_OVER_10_SECONDS = 10_001L
        const val TRIP = "group-trip"
        const val HOUSE = "group-house"
    }
}

private class FakeReviewPromptRepository(
    var state: ReviewPromptState,
) : ReviewPromptRepository {
    override suspend fun get(): ReviewPromptState = state

    override suspend fun update(transform: (ReviewPromptState) -> ReviewPromptState): ReviewPromptState =
        transform(state).also { state = it }
}
