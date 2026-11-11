package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveAllMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveShowsPixUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveShowsPixUseCaseTest {
    private lateinit var appLanguage: MutableStateFlow<AppLanguage>
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var useCase: ObserveShowsPixUseCase

    @Test
    fun `GIVEN an English app on a US device with a dollar group and no key WHEN observing THEN Pix is not offered`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val showsPix = useCase().first()

            // Then
            assertFalse(showsPix)
        }

    @Test
    fun `GIVEN the app picked in Portuguese WHEN observing THEN Pix is offered`() = runTest {
        // Given
        prepareScenario(language = AppLanguage.PORTUGUESE)

        // When
        val showsPix = useCase().first()

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN the app following a Brazilian Portuguese device WHEN observing THEN Pix is offered`() = runTest {
        // Given
        prepareScenario(
            language = AppLanguage.SYSTEM,
            deviceLanguage = "pt-BR",
            deviceRegion = null,
        )

        // When
        val showsPix = useCase().first()

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN the app following a device in Portugal WHEN observing THEN Pix is not offered`() = runTest {
        // Given
        prepareScenario(
            language = AppLanguage.SYSTEM,
            deviceLanguage = "pt-PT",
            deviceRegion = "PT",
        )

        // When
        val showsPix = useCase().first()

        // Then
        assertFalse(showsPix)
    }

    @Test
    fun `GIVEN Spanish picked on a Brazilian Portuguese device WHEN observing THEN Pix is not offered`() = runTest {
        // Given
        prepareScenario(
            language = AppLanguage.SPANISH,
            deviceLanguage = "pt-BR",
            deviceRegion = null,
        )

        // When
        val showsPix = useCase().first()

        // Then
        assertFalse(showsPix)
    }

    @Test
    fun `GIVEN a saved Pix key WHEN observing THEN Pix is offered`() = runTest {
        // Given
        prepareScenario(ownKeys = listOf(key("cpf", PaymentKeyType.CPF)))

        // When
        val showsPix = useCase().first()

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN only a Bitcoin key saved WHEN observing THEN Pix is not offered`() = runTest {
        // Given
        prepareScenario(
            ownKeys = listOf(
                key("lightning", PaymentKeyType.LIGHTNING),
                key("onchain", PaymentKeyType.ONCHAIN),
            ),
        )

        // When
        val showsPix = useCase().first()

        // Then
        assertFalse(showsPix)
    }

    @Test
    fun `GIVEN a group in reais WHEN observing THEN Pix is offered`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group("trip", GroupCurrency.USD), group("home", GroupCurrency.BRL)))

        // When
        val showsPix = useCase().first()

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN a member with a Pix key WHEN observing THEN Pix is offered`() = runTest {
        // Given
        prepareScenario(memberKeys = mapOf("ana" to listOf(key("ana-phone", PaymentKeyType.PHONE))))

        // When
        val showsPix = useCase().first()

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN members with only Bitcoin keys WHEN observing THEN Pix is not offered`() = runTest {
        // Given
        prepareScenario(memberKeys = mapOf("ana" to listOf(key("ana-lightning", PaymentKeyType.LIGHTNING))))

        // When
        val showsPix = useCase().first()

        // Then
        assertFalse(showsPix)
    }

    @Test
    fun `GIVEN a device region of Brazil WHEN observing THEN Pix is offered`() = runTest {
        // Given
        prepareScenario(deviceRegion = "br")

        // When
        val showsPix = useCase().first()

        // Then
        assertTrue(showsPix)
    }

    @Test
    fun `GIVEN Pix not offered WHEN a Pix key is saved THEN Pix becomes offered`() = runTest {
        // Given
        prepareScenario()
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        paymentKeys.saveKeys(listOf(key("cpf", PaymentKeyType.CPF)))

        // Then
        assertEquals(listOf(false, true), emitted)
    }

    @Test
    fun `GIVEN Pix not offered WHEN a group in reais is joined THEN Pix becomes offered`() = runTest {
        // Given
        prepareScenario()
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        groups.groups.value = groups.groups.value + group("barbecue", GroupCurrency.BRL)

        // Then
        assertEquals(listOf(false, true), emitted)
    }

    @Test
    fun `GIVEN Pix not offered WHEN a sync brings a Pix key of a member THEN Pix becomes offered`() = runTest {
        // Given
        prepareScenario()
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        paymentKeys.memberKeys.value = mapOf("ana" to listOf(key("ana-cpf", PaymentKeyType.CPF)))

        // Then
        assertEquals(listOf(false, true), emitted)
    }

    @Test
    fun `GIVEN Pix not offered WHEN the app is switched to Portuguese THEN Pix becomes offered`() = runTest {
        // Given
        prepareScenario()
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        appLanguage.value = AppLanguage.PORTUGUESE

        // Then
        assertEquals(listOf(false, true), emitted)
    }

    @Test
    fun `GIVEN Pix offered by a Pix key WHEN the key is deleted THEN Pix stops being offered`() = runTest {
        // Given
        prepareScenario(ownKeys = listOf(key("cpf", PaymentKeyType.CPF)))
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        paymentKeys.deleteKey(
            id = "cpf",
            nextDefault = null,
        )

        // Then
        assertEquals(listOf(true, false), emitted)
    }

    @Test
    fun `GIVEN Pix not offered WHEN a signal changes without turning it on THEN does not emit again`() = runTest {
        // Given
        prepareScenario()
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        appLanguage.value = AppLanguage.SPANISH
        paymentKeys.saveKeys(listOf(key("lightning", PaymentKeyType.LIGHTNING)))
        groups.groups.value = groups.groups.value + group("euro-trip", GroupCurrency.EUR)

        // Then
        assertEquals(listOf(false), emitted)
    }

    private fun key(
        id: String,
        type: PaymentKeyType,
    ): PaymentKey = PaymentKey(
        id = id,
        type = type,
        value = "value-of-$id",
        isDefault = true,
        addedAt = 1L,
    )

    private fun group(
        id: String,
        currency: GroupCurrency,
    ): Group = Group(
        id = id,
        name = id,
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = emptyList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 1L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun prepareScenario(
        language: AppLanguage = AppLanguage.ENGLISH,
        deviceLanguage: String? = "en-US",
        deviceRegion: String? = "US",
        ownKeys: List<PaymentKey> = emptyList(),
        memberKeys: Map<String, List<PaymentKey>> = emptyMap(),
        storedGroups: List<Group> = listOf(group("trip", GroupCurrency.USD)),
    ) {
        appLanguage = MutableStateFlow(language)
        paymentKeys = FakePaymentKeyRepository(ownKeys).also { it.memberKeys.value = memberKeys }
        groups = FakeGroupRepository(initialGroups = storedGroups)
        useCase = ObserveShowsPixUseCase(
            observeAppLanguage = { appLanguage },
            observePaymentKeys = ObservePaymentKeysUseCase(paymentKeys),
            observeAllMemberPaymentKeys = ObserveAllMemberPaymentKeysUseCase(paymentKeys),
            observeGroups = ObserveGroupsUseCase(groups),
            deviceLanguage = { deviceLanguage },
            deviceRegion = { deviceRegion },
        )
    }
}
