package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType

// Why: the Bitcoin rules of the design: mainnet bc1q and bc1p on-chain, Lightning Address or LNURL.
internal object BitcoinKeyRules {
    private const val MAINNET = "bc"
    private const val LNURL = "lnurl"
    private const val SEGWIT_MAX_LENGTH = 90

    // Why: the server's limit on a Lightning key; a longer one would be refused at every push.
    private const val LIGHTNING_MAX_LENGTH = 2048
    private const val TAPROOT_SIZE = 32
    private val SEGWIT_SIZES = setOf(20, 32)
    private val TESTNET_PREFIXES = listOf("tb1", "bcrt1")
    private val ONCHAIN_PREFIXES = listOf("bc1") + TESTNET_PREFIXES
    private val INVOICE_PREFIXES = listOf("lnbc", "lntb", "lnsb")
    private val ADDRESS = Regex("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$")

    fun validate(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyValidation = if (type == PaymentKeyType.ONCHAIN) {
        validateOnchain(input)
    } else {
        validateLightning(input)
    }

    /*
     * Why: "Colar um bc1… no campo Lightning, ou um Lightning Address no on-chain, troca a rede sozinho". The "@"
     * goes before the on-chain prefixes, which no address has, so "bc1fan@wallet.com" is still a Lightning Address.
     */
    fun detect(input: String): PaymentKeyType? {
        val text = toBareKey(input).lowercase()
        return when {
            input.trim().startsWith("bitcoin:", ignoreCase = true) -> PaymentKeyType.ONCHAIN
            input.trim().startsWith("lightning:", ignoreCase = true) -> PaymentKeyType.LIGHTNING
            '@' in text -> PaymentKeyType.LIGHTNING
            ONCHAIN_PREFIXES.any(text::startsWith) -> PaymentKeyType.ONCHAIN
            text.startsWith("ln") -> PaymentKeyType.LIGHTNING
            else -> null
        }
    }

    private fun validateOnchain(input: String): PaymentKeyValidation {
        val text = toBareKey(input)
        if (TESTNET_PREFIXES.any(text.lowercase()::startsWith)) return toInvalid(PaymentKeyError.TESTNET_ADDRESS)
        val decoded = Bech32.decode(text)?.takeIf { text.length <= SEGWIT_MAX_LENGTH && it.data.isNotEmpty() }
        val program = decoded?.let { Bech32.toBytes(it.data.copyOfRange(1, it.data.size)) }
        val version = decoded?.data?.first()
        val encoding = decoded?.encoding
        val size = program?.size
        val format = when {
            decoded == null || size == null || decoded.hrp != MAINNET -> null
            version == 0 && encoding == Bech32.Encoding.BECH32 && size in SEGWIT_SIZES -> PaymentKeyFormat.SEGWIT
            version == 1 && encoding == Bech32.Encoding.BECH32M && size == TAPROOT_SIZE -> PaymentKeyFormat.TAPROOT
            else -> null
        }
        return format?.let { PaymentKeyValidation.Valid(text.lowercase(), it) }
            ?: toInvalid(PaymentKeyError.INVALID_ONCHAIN)
    }

    private fun validateLightning(input: String): PaymentKeyValidation {
        val text = toBareKey(input).lowercase()
        return when {
            text.length > LIGHTNING_MAX_LENGTH -> toInvalid(PaymentKeyError.INVALID_LIGHTNING)

            // Why: before the invoice check, so a Lightning Address like "lnbcfan@wallet.com" stays valid.
            ADDRESS.matches(text) -> PaymentKeyValidation.Valid(text, PaymentKeyFormat.LIGHTNING_ADDRESS)

            INVOICE_PREFIXES.any(text::startsWith) -> toInvalid(PaymentKeyError.LIGHTNING_INVOICE)

            isLnurl(text) -> PaymentKeyValidation.Valid(text, PaymentKeyFormat.LNURL)

            else -> toInvalid(PaymentKeyError.INVALID_LIGHTNING)
        }
    }

    private fun isLnurl(text: String): Boolean {
        val decoded = Bech32.decode(text) ?: return false
        return decoded.hrp == LNURL && decoded.encoding == Bech32.Encoding.BECH32 &&
            Bech32.toBytes(decoded.data) != null
    }

    // Why: wallets copy "bitcoin:bc1…?amount=…" and "lightning:…"; the key is what comes after the scheme.
    private fun toBareKey(input: String): String {
        val text = input.trim()
        return when {
            text.startsWith("bitcoin:", ignoreCase = true) -> text.substring("bitcoin:".length).substringBefore('?')
            text.startsWith("lightning:", ignoreCase = true) -> text.substring("lightning:".length)
            else -> text
        }
    }

    private fun toInvalid(error: PaymentKeyError): PaymentKeyValidation = PaymentKeyValidation.Invalid(error)
}
