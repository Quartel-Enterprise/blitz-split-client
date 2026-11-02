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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterPaymentUiStateTest {
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

    private val me = PaymentPerson("member-2", "Pierre", "PI", AvatarColor.BLUE, isMe = true)
    private val ana = PaymentPerson("member-1", "Ana", "AN", AvatarColor.PEACH, isMe = false)
    private val twoGroups = listOf("floripa", "ape").map { groupId ->
        PersonGroupBalance(
            groupId = groupId,
            groupName = groupId,
            groupInitials = "GR",
            groupColor = AvatarColor.BLUE,
            memberId = "member-1",
            meMemberId = "member-2",
            amountMinor = 8_115L,
        )
    }

    private val diego = PaymentPerson("member-4", "Diego", "DI", AvatarColor.SAND, isMe = false)

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
        keyMethod: PaymentMethod = PaymentMethod.PIX,
        offersReceiverKeys: Boolean = true,
    ): RegisterPaymentUiState = RegisterPaymentUiState(
        currency = currency,
        from = from,
        to = to,
        debtMinor = debtMinor,
        groupParts = groupParts,
        amountText = amountText,
        hasEditedAmount = false,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        paidDay = RelativeDay.TODAY,
        latestPaidAt = 0L,
        isOnline = isOnline,
        isSaving = isSaving,
        isLoaded = isLoaded,
        receiverKeys = receiverKeys,
        groupName = null,
        keyMethod = keyMethod,
        offersReceiverKeys = offersReceiverKeys,
        pixCode = null,
        pixQrCode = null,
    )
}
