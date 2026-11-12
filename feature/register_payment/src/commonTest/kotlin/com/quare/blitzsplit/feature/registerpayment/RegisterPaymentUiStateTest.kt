package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifyChangedNotice
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterPaymentUiStateTest {
    @Test
    fun `GIVEN the switch changed under the form WHEN reading whether it can pay anyway THEN it can`() {
        // Given
        val state = state(simplifyChange = switchedOn)

        // When
        val canPayAnyway = state.canPayAnyway

        // Then
        assertTrue(canPayAnyway)
    }

    @Test
    fun `GIVEN the switch changed and the payment being saved WHEN reading whether it can pay anyway THEN it cannot`() {
        // Given
        val state = state(
            simplifyChange = switchedOn,
            isSaving = true,
        )

        // When
        val canPayAnyway = state.canPayAnyway

        // Then
        assertFalse(canPayAnyway)
    }

    @Test
    fun `GIVEN no switch change WHEN reading whether it can pay anyway THEN it cannot`() {
        // Given
        val state = state(simplifyChange = null)

        // When
        val canPayAnyway = state.canPayAnyway

        // Then
        assertFalse(canPayAnyway)
    }

    @Test
    fun `GIVEN the device user receives WHEN reading the kind THEN it is received`() {
        // Given
        val state = state(
            from = ana,
            to = me,
        )

        // When
        val kind = state.kind

        // Then
        assertEquals(PaymentKind.RECEIVED, kind)
    }

    @Test
    fun `GIVEN the device user pays WHEN reading the kind THEN it is paid`() {
        // Given
        val state = state(
            from = me,
            to = ana,
        )

        // When
        val kind = state.kind

        // Then
        assertEquals(PaymentKind.PAID, kind)
    }

    @Test
    fun `GIVEN two other members WHEN reading the kind THEN it is between others`() {
        // Given
        val state = state(
            from = diego,
            to = ana,
        )

        // When
        val kind = state.kind

        // Then
        assertEquals(PaymentKind.BETWEEN_OTHERS, kind)
    }

    @Test
    fun `GIVEN a valid amount but nothing loaded WHEN reading whether it can confirm THEN it cannot`() {
        // Given
        val state = state(
            amountText = "100,00",
            isLoaded = false,
        )

        // When
        val canConfirm = state.canConfirm

        // Then
        assertFalse(canConfirm)
    }

    @Test
    fun `GIVEN a valid amount while saving WHEN reading whether it can confirm THEN it cannot`() {
        // Given
        val state = state(
            amountText = "100,00",
            isSaving = true,
        )

        // When
        val canConfirm = state.canConfirm

        // Then
        assertFalse(canConfirm)
    }

    @Test
    fun `GIVEN a partial amount WHEN reading the state THEN it can confirm and shows what is left`() {
        // Given
        val state = state(amountText = "100,00")

        // When
        val remaining = state.remainingMinor

        // Then
        assertEquals(6_230L, remaining)
        assertTrue(state.canConfirm)
        assertFalse(state.isFullDebt)
        assertFalse(state.showsOverDebtError)
    }

    @Test
    fun `GIVEN text that is no amount WHEN reading the state THEN it is neither the whole debt nor over it`() {
        // Given
        val state = state(amountText = ".")

        // When
        val amount = state.amountMinor

        // Then
        assertNull(amount)
        assertFalse(state.isAmountValid)
        assertFalse(state.isFullDebt)
        assertFalse(state.showsOverDebtError)
        assertNull(state.remainingMinor)
    }

    @Test
    fun `GIVEN an amount over the debt WHEN reading the state THEN shows the error and nothing left`() {
        // Given
        val state = state(amountText = "500")

        // When
        val showsError = state.showsOverDebtError

        // Then
        assertTrue(showsError)
        assertFalse(state.isAmountValid)
        assertNull(state.remainingMinor)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `GIVEN a settling up across groups WHEN typing less than the net THEN the amount is not valid`() {
        // Given
        val state = state(
            amountText = "100,00",
            groupParts = twoGroups,
        )

        // When
        val isValid = state.isAmountValid

        // Then
        assertTrue(state.isAmountLocked)
        assertFalse(isValid)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `GIVEN a settling up across groups WHEN the amount is the whole net THEN it can be confirmed`() {
        // Given
        val state = state(
            amountText = "162,30",
            groupParts = twoGroups,
        )

        // When
        val canConfirm = state.canConfirm

        // Then
        assertTrue(canConfirm)
    }

    @Test
    fun `GIVEN a pair of one group WHEN reading the amount THEN it is not locked`() {
        // Given
        val state = state()

        // When
        val isLocked = state.isAmountLocked

        // Then
        assertFalse(isLocked)
    }

    @Test
    fun `GIVEN a group in yen WHEN typing the debt with a thousands dot THEN it is the whole debt`() {
        // Given
        val state = state(
            currency = GroupCurrency.JPY,
            debtMinor = 1_500L,
            amountText = "1.500",
        )

        // When
        val amount = state.amountMinor

        // Then
        assertEquals(1_500L, amount)
        assertTrue(state.isFullDebt)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `GIVEN offline WHEN reading the state THEN shows the offline notice`() {
        // Given
        val state = state(isOnline = false)

        // When
        val showsNotice = state.showsOfflineNotice

        // Then
        assertTrue(showsNotice)
    }

    @Test
    fun `GIVEN this device user pays someone with a key WHEN reading the state THEN shows the receiver keys`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone),
        )

        // When
        val showsKeys = state.showsReceiverKeys

        // Then
        assertTrue(showsKeys)
        assertTrue(state.canMarkAsPaid)
    }

    @Test
    fun `GIVEN a route that offers no keys WHEN reading the state THEN shows the form`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone),
            offersReceiverKeys = false,
        )

        // When
        val showsKeys = state.showsReceiverKeys

        // Then
        assertFalse(showsKeys)
        assertFalse(state.canMarkAsPaid)
    }

    @Test
    fun `GIVEN the device user receives from someone with keys WHEN reading the state THEN shows the form`() {
        // Given
        val state = state(
            from = ana,
            to = me,
            receiverKeys = listOf(pixPhone),
        )

        // When
        val showsKeys = state.showsReceiverKeys

        // Then
        assertFalse(showsKeys)
        assertFalse(state.canMarkAsPaid)
    }

    @Test
    fun `GIVEN two others with a receiver key WHEN reading the state THEN shows the form`() {
        // Given
        val state = state(
            from = diego,
            to = ana,
            receiverKeys = listOf(pixPhone),
        )

        // When
        val showsKeys = state.showsReceiverKeys

        // Then
        assertFalse(showsKeys)
        assertFalse(state.canMarkAsPaid)
    }

    @Test
    fun `GIVEN a receiver with no key WHEN this device user pays THEN shows the form`() {
        // Given
        val state = state(
            from = me,
            to = ana,
        )

        // When
        val showsKeys = state.showsReceiverKeys

        // Then
        assertFalse(showsKeys)
        assertFalse(state.canMarkAsPaid)
        assertNull(state.defaultKey)
        assertTrue(state.otherKeys.isEmpty())
    }

    @Test
    fun `GIVEN Bitcoin keys listed first WHEN reading the methods THEN Pix comes first and the picker shows`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(lightning, pixPhone),
        )

        // When
        val methods = state.keyMethods

        // Then
        assertEquals(listOf(PaymentMethod.PIX, PaymentMethod.BITCOIN), methods)
        assertTrue(state.showsKeyMethodPicker)
        assertEquals(PaymentMethod.PIX, state.shownKeyMethod)
    }

    @Test
    fun `GIVEN only Pix keys WHEN reading the methods THEN the picker does not show`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone, pixEmail),
        )

        // When
        val methods = state.keyMethods

        // Then
        assertEquals(listOf(PaymentMethod.PIX), methods)
        assertFalse(state.showsKeyMethodPicker)
    }

    @Test
    fun `GIVEN only Bitcoin keys and Pix picked WHEN reading the shown method THEN it is Bitcoin without a picker`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(lightning, onchain),
            keyMethod = PaymentMethod.PIX,
        )

        // When
        val shown = state.shownKeyMethod

        // Then
        assertEquals(PaymentMethod.BITCOIN, shown)
        assertFalse(state.showsKeyMethodPicker)
        assertEquals(lightning, state.defaultKey)
        assertEquals(listOf(onchain), state.otherKeys)
    }

    @Test
    fun `GIVEN both methods and Bitcoin picked WHEN reading the keys THEN shows the Bitcoin default and others`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone, pixEmail, lightning, onchain),
            keyMethod = PaymentMethod.BITCOIN,
        )

        // When
        val defaultKey = state.defaultKey

        // Then
        assertEquals(PaymentMethod.BITCOIN, state.shownKeyMethod)
        assertEquals(lightning, defaultKey)
        assertEquals(listOf(onchain), state.otherKeys)
    }

    @Test
    fun `GIVEN Pix keys with the second one flagged WHEN reading the keys THEN the flagged one is the default`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixEmail, pixPhone, pixRandom),
        )

        // When
        val defaultKey = state.defaultKey

        // Then
        assertEquals(pixPhone, defaultKey)
        assertEquals(listOf(pixEmail, pixRandom), state.otherKeys)
    }

    @Test
    fun `GIVEN Pix keys none of them flagged WHEN reading the keys THEN the first one is the default`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixEmail, pixRandom),
        )

        // When
        val defaultKey = state.defaultKey

        // Then
        assertEquals(pixEmail, defaultKey)
        assertEquals(listOf(pixRandom), state.otherKeys)
    }

    @Test
    fun `GIVEN Pix shown with Bitcoin keys too WHEN reading the other keys THEN lists only the other Pix keys`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone, pixEmail, lightning, onchain),
        )

        // When
        val otherKeys = state.otherKeys

        // Then
        assertEquals(listOf(pixEmail), otherKeys)
    }

    @Test
    fun `GIVEN a Pix phone key and 98 reais in reais WHEN reading the expected code THEN is the known copia e cola`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            debtMinor = 9_800L,
            amountText = "98,00",
            receiverKeys = listOf(pixPhone),
        )

        // When
        val code = state.expectedPixCode

        // Then
        assertEquals(
            "00020126360014br.gov.bcb.pix0114+5521987654321520400005303986540598.005802BR5903Ana6006BRASIL" +
                "62070503***6304D3C1",
            code,
        )
    }

    @Test
    fun `GIVEN Bitcoin shown WHEN reading the expected code THEN there is no Pix code`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone, lightning),
            keyMethod = PaymentMethod.BITCOIN,
        )

        // When
        val code = state.expectedPixCode

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a group in dollars WHEN reading the expected code THEN there is no Pix code`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            currency = GroupCurrency.USD,
            amountText = "162.30",
            receiverKeys = listOf(pixPhone),
        )

        // When
        val code = state.expectedPixCode

        // Then
        assertNull(code)
        assertTrue(state.showsReceiverKeys)
    }

    @Test
    fun `GIVEN keys shown while saving WHEN reading whether it can mark as paid THEN it cannot`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone),
            isSaving = true,
        )

        // When
        val canMarkAsPaid = state.canMarkAsPaid

        // Then
        assertFalse(canMarkAsPaid)
    }

    @Test
    fun `GIVEN keys but nothing loaded WHEN reading whether it can mark as paid THEN it cannot`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone),
            isLoaded = false,
        )

        // When
        val canMarkAsPaid = state.canMarkAsPaid

        // Then
        assertFalse(canMarkAsPaid)
    }

    @Test
    fun `GIVEN this device user pays by Pix in Quem pagou a quem WHEN reading the form keys THEN lists only Pix`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixEmail, lightning, pixPhone, pixCpf, onchain, pixRandom),
            offersReceiverKeys = false,
        )

        // When
        val formKeys = state.formPixKeys

        // Then
        assertEquals(listOf(pixPhone, pixEmail, pixCpf, pixRandom), formKeys)
        assertFalse(state.showsNoPixKeyNotice)
    }

    @Test
    fun `GIVEN Paguei outro valor picked over Pagar Ana WHEN reading the form keys THEN lists the Pix keys`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone, pixEmail, lightning),
            prefersForm = true,
        )

        // When
        val formKeys = state.formPixKeys

        // Then
        assertFalse(state.showsReceiverKeys)
        assertEquals(listOf(pixPhone, pixEmail), formKeys)
        assertFalse(state.showsNoPixKeyNotice)
    }

    @Test
    fun `GIVEN Pix keys none of them flagged WHEN reading the form keys THEN keeps the order they came in`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixRandom, pixEmail, pixCpf),
            offersReceiverKeys = false,
        )

        // When
        val formKeys = state.formPixKeys

        // Then
        assertEquals(listOf(pixRandom, pixEmail, pixCpf), formKeys)
    }

    @Test
    fun `GIVEN Pagar Ana showing the keys WHEN reading the form keys THEN the form lists none`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone, pixEmail),
        )

        // When
        val formKeys = state.formPixKeys

        // Then
        assertTrue(state.showsReceiverKeys)
        assertTrue(formKeys.isEmpty())
        assertFalse(state.showsNoPixKeyNotice)
    }

    @Test
    fun `GIVEN a receiver with no key WHEN this device user pays by Pix THEN shows the no Pix key notice`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = emptyList(),
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertTrue(showsNotice)
        assertTrue(state.formPixKeys.isEmpty())
    }

    @Test
    fun `GIVEN a dollar group and a receiver with no key WHEN this device user pays by Pix THEN shows no notice`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = emptyList(),
            currency = GroupCurrency.USD,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertFalse(showsNotice)
    }

    @Test
    fun `GIVEN a dollar group and a receiver with a Pix key WHEN this device user pays by Pix THEN lists it`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone),
            currency = GroupCurrency.USD,
            offersReceiverKeys = false,
        )

        // When
        val keys = state.formPixKeys

        // Then
        assertEquals(listOf(pixPhone), keys)
    }

    @Test
    fun `GIVEN the keys of a receiver unknown offline WHEN this device user pays by Pix THEN shows no notice`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = emptyList(),
            areReceiverKeysUnknown = true,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertFalse(showsNotice)
    }

    @Test
    fun `GIVEN a receiver with only Bitcoin keys and the form picked WHEN paying by Pix THEN shows the notice`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(lightning, onchain),
            prefersForm = true,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertTrue(showsNotice)
        assertTrue(state.formPixKeys.isEmpty())
    }

    @Test
    fun `GIVEN only Bitcoin keys in Quem pagou a quem WHEN paying by Pix THEN shows the notice`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            receiverKeys = listOf(lightning),
            offersReceiverKeys = false,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertTrue(showsNotice)
        assertTrue(state.formPixKeys.isEmpty())
    }

    @Test
    fun `GIVEN a receiver with Pix keys WHEN paying another way than Pix THEN the form lists no key`() {
        // Given
        val methods = listOf(SettlementMethod.CASH, SettlementMethod.TRANSFER, SettlementMethod.OTHER)

        // When
        val states = methods.map { method ->
            state(
                from = me,
                to = ana,
                receiverKeys = listOf(pixPhone),
                prefersForm = true,
                method = method,
            )
        }

        // Then
        states.forEach { state ->
            assertTrue(state.formPixKeys.isEmpty(), "${state.method}")
            assertFalse(state.showsNoPixKeyNotice, "${state.method}")
        }
    }

    @Test
    fun `GIVEN a receiver with no key WHEN paying another way than Pix THEN shows no notice`() {
        // Given
        val methods = listOf(SettlementMethod.CASH, SettlementMethod.TRANSFER, SettlementMethod.OTHER)

        // When
        val states = methods.map { method ->
            state(
                from = me,
                to = ana,
                method = method,
            )
        }

        // Then
        states.forEach { state ->
            assertFalse(state.showsNoPixKeyNotice, "${state.method}")
            assertTrue(state.formPixKeys.isEmpty(), "${state.method}")
        }
    }

    @Test
    fun `GIVEN the device user receives by Pix WHEN reading the form keys THEN lists none and shows no notice`() {
        // Given
        val state = state(
            from = ana,
            to = me,
            receiverKeys = listOf(pixPhone),
            offersReceiverKeys = false,
        )

        // When
        val formKeys = state.formPixKeys

        // Then
        assertTrue(formKeys.isEmpty())
        assertFalse(state.showsNoPixKeyNotice)
    }

    @Test
    fun `GIVEN the device user receives from someone with no key WHEN reading the notice THEN it does not show`() {
        // Given
        val state = state(
            from = ana,
            to = me,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertFalse(showsNotice)
    }

    @Test
    fun `GIVEN two others paying by Pix WHEN reading the form keys THEN lists none and shows no notice`() {
        // Given
        val withKeys = state(
            from = diego,
            to = ana,
            receiverKeys = listOf(pixPhone),
        )
        val withoutKeys = state(
            from = diego,
            to = ana,
        )

        // When
        val formKeys = withKeys.formPixKeys

        // Then
        assertTrue(formKeys.isEmpty())
        assertFalse(withKeys.showsNoPixKeyNotice)
        assertFalse(withoutKeys.showsNoPixKeyNotice)
    }

    @Test
    fun `GIVEN a receiver no longer in the group WHEN paying by Pix THEN lists no key and shows no notice`() {
        // Given
        val state = state(
            from = me,
            to = null,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertEquals(PaymentKind.PAID, state.kind)
        assertFalse(showsNotice)
        assertTrue(state.formPixKeys.isEmpty())
    }

    @Test
    fun `GIVEN nothing loaded yet WHEN reading the form keys THEN lists none and shows no notice`() {
        // Given
        val withKeys = state(
            from = me,
            to = ana,
            receiverKeys = listOf(pixPhone),
            offersReceiverKeys = false,
            isLoaded = false,
        )
        val withoutKeys = state(
            from = me,
            to = ana,
            isLoaded = false,
        )

        // When
        val formKeys = withKeys.formPixKeys

        // Then
        assertTrue(formKeys.isEmpty())
        assertFalse(withKeys.showsNoPixKeyNotice)
        assertFalse(withoutKeys.showsNoPixKeyNotice)
    }

    @Test
    fun `GIVEN the receiver keys still loading under the form WHEN reading the notice THEN it does not show`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            prefersForm = true,
            keys = Loadable.Loading,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertFalse(showsNotice)
        assertTrue(state.formPixKeys.isEmpty())
    }

    @Test
    fun `GIVEN the receiver keys shimmering WHEN reading the notice THEN it does not show`() {
        // Given
        val state = state(
            from = me,
            to = ana,
            keys = Loadable.Loading,
        )

        // When
        val showsNotice = state.showsNoPixKeyNotice

        // Then
        assertTrue(state.isLoadingReceiverKeys)
        assertFalse(showsNotice)
        assertTrue(state.formPixKeys.isEmpty())
    }

    @Test
    fun `GIVEN a form not loaded yet with everything changed WHEN reading the unsaved input THEN there is none`() {
        // Given
        val state = state(
            isLoaded = false,
            hasEditedAmount = true,
            amountText = "10,00",
            method = SettlementMethod.CASH,
            paidAt = 5L,
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
    }

    @Test
    fun `GIVEN a form just opened WHEN reading the unsaved input THEN there is none`() {
        // Given
        val state = state()

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
    }

    @Test
    fun `GIVEN a part of the debt typed WHEN reading the unsaved input THEN there is some`() {
        // Given
        val state = state(
            hasEditedAmount = true,
            amountText = "10,00",
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertTrue(hasUnsavedInput)
    }

    @Test
    fun `GIVEN the amount cleared WHEN reading the unsaved input THEN there is some`() {
        // Given
        val state = state(
            hasEditedAmount = true,
            amountText = "",
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertTrue(hasUnsavedInput)
    }

    @Test
    fun `GIVEN the whole debt typed again WHEN reading the unsaved input THEN there is none`() {
        // Given
        val state = state(
            hasEditedAmount = true,
            amountText = "162,30",
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
    }

    @Test
    fun `GIVEN an amount that differs but was never edited WHEN reading the unsaved input THEN there is none`() {
        // Given
        val state = state(
            hasEditedAmount = false,
            amountText = "10,00",
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
    }

    @Test
    fun `GIVEN another method WHEN reading the unsaved input THEN there is some`() {
        // Given
        val state = state(
            method = SettlementMethod.CASH,
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertTrue(hasUnsavedInput)
    }

    @Test
    fun `GIVEN Pix WHEN reading the unsaved input THEN there is none`() {
        // Given
        val state = state(
            method = SettlementMethod.PIX,
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
    }

    @Test
    fun `GIVEN another Quando than the one the form opened with WHEN reading the unsaved input THEN there is some`() {
        // Given
        val state = state(
            paidAt = 5L,
            openedPaidAt = 10L,
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertTrue(hasUnsavedInput)
    }

    @Test
    fun `GIVEN the Quando the form opened with WHEN reading the unsaved input THEN there is none`() {
        // Given
        val state = state(
            paidAt = 10L,
            openedPaidAt = 10L,
        )

        // When
        val hasUnsavedInput = state.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
    }

    private val me = PaymentPerson(
        "member-2",
        "Pierre",
        "PI",
        AvatarColor.BLUE,
        photoUrl = null,
        isMe = true,
        personKey = "name:pierre",
    )
    private val ana = PaymentPerson(
        "member-1",
        "Ana",
        "AN",
        AvatarColor.PEACH,
        photoUrl = null,
        isMe = false,
        personKey = "name:ana",
    )
    private val twoGroups = listOf("floripa", "ape").map { groupId ->
        PersonGroupBalance(
            groupId = groupId,
            groupName = groupId,
            groupInitials = "GR",
            groupColor = AvatarColor.BLUE,
            memberId = "member-1",
            meMemberId = "member-2",
            amountMinor = 8_115L,
            isSimplified = false,
            insteadOfNames = emptyList(),
        )
    }

    private val diego = PaymentPerson(
        "member-4",
        "Diego",
        "DI",
        AvatarColor.SAND,
        photoUrl = null,
        isMe = false,
        personKey = "name:diego",
    )

    private val switchedOn = SimplifyChangedNotice(
        changedByName = "Ana",
        simplifiesDebts = true,
        groupName = "Viagem Floripa",
        previousDebtMinor = 8_000L,
    )

    @Test
    fun `GIVEN the Pix rule on WHEN listing the methods THEN lists Pix first and the others`() {
        // Given
        val state = state(
            showsPix = true,
            method = SettlementMethod.CASH,
        )

        // When
        val methods = state.methods

        // Then
        assertEquals(
            listOf(SettlementMethod.PIX, SettlementMethod.CASH, SettlementMethod.TRANSFER, SettlementMethod.OTHER),
            methods,
        )
    }

    @Test
    fun `GIVEN the Pix rule off and Transfer picked WHEN listing the methods THEN hides Pix`() {
        // Given
        val state = state(
            showsPix = false,
            method = SettlementMethod.TRANSFER,
        )

        // When
        val methods = state.methods

        // Then
        assertEquals(listOf(SettlementMethod.CASH, SettlementMethod.TRANSFER, SettlementMethod.OTHER), methods)
    }

    @Test
    fun `GIVEN the Pix rule off and Pix picked WHEN listing the methods THEN still lists Pix`() {
        // Given
        val state = state(
            showsPix = false,
            method = SettlementMethod.PIX,
        )

        // When
        val methods = state.methods

        // Then
        assertEquals(
            listOf(SettlementMethod.PIX, SettlementMethod.CASH, SettlementMethod.TRANSFER, SettlementMethod.OTHER),
            methods,
        )
    }

    private val pixCpf = key("cpf", PaymentKeyType.CPF, "12345678909")
    private val pixPhone = key("phone", PaymentKeyType.PHONE, "+5521987654321", isDefault = true)
    private val pixEmail = key("email", PaymentKeyType.EMAIL, "ana@email.com")
    private val pixRandom = key("random", PaymentKeyType.RANDOM, "123e4567-e89b-12d3-a456-426614174000")
    private val lightning = key("lightning", PaymentKeyType.LIGHTNING, "ana@walletofsatoshi.com", isDefault = true)
    private val onchain = key("onchain", PaymentKeyType.ONCHAIN, "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")

    private fun key(
        id: String,
        type: PaymentKeyType,
        value: String,
        isDefault: Boolean = false,
    ): ReceiverKey = ReceiverKey(
        id = id,
        type = type,
        value = value,
        text = value,
        isDefault = isDefault,
    )

    private fun state(
        from: PaymentPerson? = ana,
        to: PaymentPerson? = me,
        currency: GroupCurrency = GroupCurrency.BRL,
        debtMinor: Long = 16_230L,
        amountText: String = "162,30",
        isOnline: Boolean = true,
        isSaving: Boolean = false,
        isLoaded: Boolean = true,
        groupParts: List<PersonGroupBalance> = emptyList(),
        receiverKeys: List<ReceiverKey> = emptyList(),
        keys: Loadable<List<ReceiverKey>> = receiverKeys.toLoaded(),
        method: SettlementMethod = SettlementMethod.PIX,
        prefersForm: Boolean = false,
        keyMethod: PaymentMethod = PaymentMethod.PIX,
        offersReceiverKeys: Boolean = true,
        simplifyChange: SimplifyChangedNotice? = null,
        areReceiverKeysUnknown: Boolean = false,
        hasEditedAmount: Boolean = false,
        paidAt: Long = 0L,
        openedPaidAt: Long = 0L,
        openedMethod: SettlementMethod = SettlementMethod.PIX,
        showsPix: Boolean = true,
    ): RegisterPaymentUiState = RegisterPaymentUiState(
        currency = currency,
        showsSats = false,
        from = from,
        to = to,
        debtMinor = debtMinor,
        groupParts = groupParts,
        amountText = amountText,
        hasEditedAmount = hasEditedAmount,
        method = method,
        paidAt = paidAt,
        openedPaidAt = openedPaidAt,
        openedMethod = openedMethod,
        paidDay = RelativeDay.TODAY,
        latestPaidAt = 0L,
        isOnline = isOnline,
        isSaving = isSaving,
        isLoaded = isLoaded,
        receiverKeys = keys,
        areReceiverKeysUnknown = areReceiverKeysUnknown,
        groupName = null,
        keyMethod = keyMethod,
        showsPix = showsPix,
        offersReceiverKeys = offersReceiverKeys,
        prefersForm = prefersForm,
        pixCode = null,
        pixQrCode = null,
        simplifiedNote = null,
        simplifyChange = simplifyChange,
    )
}
