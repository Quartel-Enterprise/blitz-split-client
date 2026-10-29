package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.model.PaymentKeyValidation
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidatePaymentKeyUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

// The Bitcoin rules of ValidatePaymentKeyUseCase, over the test vectors of BIP 173 and BIP 350 and LUD-01.
class ValidateBitcoinKeyUseCaseTest {
    private val useCase = ValidatePaymentKeyUseCase()

    @Test
    fun `GIVEN a bc1q address of a 20 byte program WHEN validating on-chain THEN is SegWit`() {
        // Given
        val input = SEGWIT_P2WPKH

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(SEGWIT_P2WPKH, PaymentKeyFormat.SEGWIT), result)
    }

    @Test
    fun `GIVEN a bc1q address of a 32 byte program WHEN validating on-chain THEN is SegWit`() {
        // Given
        val input = SEGWIT_P2WSH

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(SEGWIT_P2WSH, PaymentKeyFormat.SEGWIT), result)
    }

    @Test
    fun `GIVEN a bc1p address WHEN validating on-chain THEN is Taproot`() {
        // Given
        val input = TAPROOT

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(TAPROOT, PaymentKeyFormat.TAPROOT), result)
    }

    @Test
    fun `GIVEN an address in upper case WHEN validating on-chain THEN stores it in lower case`() {
        // Given
        val input = SEGWIT_P2WPKH.uppercase()

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(SEGWIT_P2WPKH, PaymentKeyFormat.SEGWIT), result)
    }

    @Test
    fun `GIVEN a Taproot address in upper case WHEN validating on-chain THEN stores it in lower case`() {
        // Given
        val input = TAPROOT.uppercase()

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(TAPROOT, PaymentKeyFormat.TAPROOT), result)
    }

    @Test
    fun `GIVEN an address in mixed case WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "bc1qW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN an address with one character mistyped WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a bc1p address with a bech32 checksum WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqh2y7hd"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a bc1q address with a bech32m checksum WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a bc1q address of a 16 byte program WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a bc1p address of a 40 byte program WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kt5nd6y"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a witness version 2 address WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "bc1zw508d6qejxtdg4y5r3zarvaryvaxxpcs"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a tb1 address WHEN validating on-chain THEN is a testnet address`() {
        // Given
        val input = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.TESTNET_ADDRESS), result)
    }

    @Test
    fun `GIVEN a bcrt1 address WHEN validating on-chain THEN is a testnet address`() {
        // Given
        val input = "bcrt1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.TESTNET_ADDRESS), result)
    }

    @Test
    fun `GIVEN a legacy address starting with 1 WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a legacy address starting with 3 WHEN validating on-chain THEN is an invalid address`() {
        // Given
        val input = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_ONCHAIN), result)
    }

    @Test
    fun `GIVEN a bitcoin URI with an amount WHEN validating on-chain THEN stores only its address`() {
        // Given
        val input = "bitcoin:$SEGWIT_P2WPKH?amount=0.001"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(SEGWIT_P2WPKH, PaymentKeyFormat.SEGWIT), result)
    }

    @Test
    fun `GIVEN a bitcoin URI in upper case WHEN validating on-chain THEN stores its address in lower case`() {
        // Given
        val input = "BITCOIN:${TAPROOT.uppercase()}"

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(TAPROOT, PaymentKeyFormat.TAPROOT), result)
    }

    @Test
    fun `GIVEN a Lightning Address in upper case WHEN validating Lightning THEN stores it in lower case`() {
        // Given
        val input = "Pierre@WalletOfSatoshi.com"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(
            PaymentKeyValidation.Valid("pierre@walletofsatoshi.com", PaymentKeyFormat.LIGHTNING_ADDRESS),
            result,
        )
    }

    @Test
    fun `GIVEN a Lightning Address with the lightning prefix WHEN validating Lightning THEN drops the prefix`() {
        // Given
        val input = "lightning:pierre@getalby.com"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid("pierre@getalby.com", PaymentKeyFormat.LIGHTNING_ADDRESS), result)
    }

    @Test
    fun `GIVEN an LNURL in upper case WHEN validating Lightning THEN stores it in lower case`() {
        // Given
        val input = LNURL.uppercase()

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(LNURL, PaymentKeyFormat.LNURL), result)
    }

    @Test
    fun `GIVEN an LNURL with the lightning prefix WHEN validating Lightning THEN drops the prefix`() {
        // Given
        val input = "lightning:${LNURL.uppercase()}"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Valid(LNURL, PaymentKeyFormat.LNURL), result)
    }

    @Test
    fun `GIVEN an LNURL with one character mistyped WHEN validating Lightning THEN is an invalid Lightning key`() {
        // Given
        val input = LNURL.dropLast(1) + "q"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_LIGHTNING), result)
    }

    @Test
    fun `GIVEN an LNURL with a bech32m checksum WHEN validating Lightning THEN is an invalid Lightning key`() {
        // Given
        val input = LNURL_BECH32M

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_LIGHTNING), result)
    }

    @Test
    fun `GIVEN a mainnet invoice WHEN validating Lightning THEN is an invoice`() {
        // Given
        val input = "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.LIGHTNING_INVOICE), result)
    }

    @Test
    fun `GIVEN a testnet invoice with the lightning prefix WHEN validating Lightning THEN is an invoice`() {
        // Given
        val input = "lightning:LNTB20M1PVJLUEZPP5QQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQYPQ"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.LIGHTNING_INVOICE), result)
    }

    @Test
    fun `GIVEN a name without a wallet WHEN validating Lightning THEN is an invalid Lightning key`() {
        // Given
        val input = "pierre"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_LIGHTNING), result)
    }

    @Test
    fun `GIVEN a Lightning Address without a top level domain WHEN validating Lightning THEN is invalid`() {
        // Given
        val input = "pierre@getalby"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_LIGHTNING), result)
    }

    @Test
    fun `GIVEN a Lightning Address longer than the server takes WHEN validating Lightning THEN is invalid`() {
        // Given
        val input = "a".repeat(2_040) + "@wallet.com"

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_LIGHTNING), result)
    }

    @Test
    fun `GIVEN an on-chain address WHEN validating Lightning THEN is an invalid Lightning key`() {
        // Given
        val input = SEGWIT_P2WPKH

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, input, otherKeys = emptyList())

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_LIGHTNING), result)
    }

    @Test
    fun `GIVEN the address of another saved key in upper case WHEN validating on-chain THEN is already saved`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.ONCHAIN, SEGWIT_P2WPKH))

        // When
        val result = useCase(PaymentKeyType.ONCHAIN, SEGWIT_P2WPKH.uppercase(), otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.ALREADY_SAVED), result)
    }

    @Test
    fun `GIVEN the Lightning Address of another saved key WHEN validating it with the prefix THEN is already saved`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.LIGHTNING, "pierre@getalby.com"))

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, "lightning:Pierre@GetAlby.com", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Invalid(PaymentKeyError.ALREADY_SAVED), result)
    }

    @Test
    fun `GIVEN the same text saved as a Pix email WHEN validating it as a Lightning Address THEN is valid`() {
        // Given
        val otherKeys = listOf(key(PaymentKeyType.EMAIL, "pierre@getalby.com"))

        // When
        val result = useCase(PaymentKeyType.LIGHTNING, "pierre@getalby.com", otherKeys)

        // Then
        assertEquals(PaymentKeyValidation.Valid("pierre@getalby.com", PaymentKeyFormat.LIGHTNING_ADDRESS), result)
    }

    private fun key(
        type: PaymentKeyType,
        value: String,
    ): PaymentKey = PaymentKey(
        id = "key-1",
        type = type,
        value = value,
        isDefault = true,
        addedAt = 1L,
    )

    private companion object {
        const val SEGWIT_P2WPKH = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        const val SEGWIT_P2WSH = "bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3"
        const val TAPROOT = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0"
        const val LNURL =
            "lnurl1dp68gurn8ghj7um9wfmxjcm99e3k7mf0v9cxj0m385ekvcenxc6r2c35xvukxefcv5mkvv34x5ekzd3ev56nyd3hxqu" +
                "rzepexejxxepnxscrvwfnv9nxzcn9xq6xyefhvgcxxcmyxymnserxfq5fns"
        const val LNURL_BECH32M =
            "lnurl1dp68gurn8ghj7um9wfmxjcm99e3k7mf0v9cxj0m385ekvcenxc6r2c35xvukxefcv5mkvv34x5ekzd3ev56nyd3hxqu" +
                "rzepexejxxepnxscrvwfnv9nxzcn9xq6xyefhvgcxxcmyxymnserxuuy9kj"
    }
}
