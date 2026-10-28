package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType

// Why: the Pix rules of the design: each type with its mask and its own check.
internal object PixKeyRules {
    private const val CPF_LENGTH = 11
    private const val PHONE_LENGTH = 11
    private const val COUNTRY_CODE = "55"
    private const val EMAIL_MAX_LENGTH = 77
    private const val RANDOM_LENGTH = 32

    // Why: the DDDs Anatel assigns; a made-up one would never get a payment.
    private val DDDS: Set<Int> = (
        (11..19) + listOf(21, 22, 24, 27, 28) + listOf(31, 32, 33, 34, 35, 37, 38) + (41..49) +
            listOf(51, 53, 54, 55) + (61..69) + listOf(71, 73, 74, 75, 77, 79) + (81..89) + (91..99)
    ).toSet()

    private val EMAIL = Regex("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$")
    private val UUID = Regex("^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$")
    private val NUMBER = Regex("^[0-9.\\-\\s()]+$")

    fun validate(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyValidation = when (type) {
        PaymentKeyType.CPF -> validateCpf(input)
        PaymentKeyType.PHONE -> validatePhone(input)
        PaymentKeyType.EMAIL -> validateEmail(input)
        else -> validateRandom(input)
    }

    // Why: on a paste or a typed key, only a shape that belongs to one type switches to it.
    fun detect(input: String): PaymentKeyType? {
        val text = input.trim()
        val digits = text.filter(Char::isDigit)
        return when {
            '@' in text -> PaymentKeyType.EMAIL
            UUID.matches(text.lowercase()) && digits.length < RANDOM_LENGTH -> PaymentKeyType.RANDOM
            text.startsWith("+") -> PaymentKeyType.PHONE
            !NUMBER.matches(text) -> null
            digits.length == CPF_LENGTH && isValidCpf(digits) -> PaymentKeyType.CPF
            digits.length == CPF_LENGTH + COUNTRY_CODE.length && digits.startsWith(COUNTRY_CODE) -> PaymentKeyType.PHONE
            else -> null
        }
    }

    fun formatInput(
        type: PaymentKeyType,
        input: String,
    ): PaymentKeyInput = when (type) {
        PaymentKeyType.CPF -> {
            val digits = input.filter(Char::isDigit).take(CPF_LENGTH)
            PaymentKeyInput(
                text = formatCpf(digits),
                isComplete = digits.length == CPF_LENGTH,
            )
        }

        PaymentKeyType.PHONE -> {
            val digits = extractPhoneDigits(input).take(PHONE_LENGTH)
            PaymentKeyInput(
                text = formatPhone(digits),
                isComplete = digits.length == PHONE_LENGTH,
            )
        }

        PaymentKeyType.RANDOM -> PaymentKeyInput(
            text = input,
            isComplete = input.count { it.isLetterOrDigit() } >= RANDOM_LENGTH,
        )

        else -> PaymentKeyInput(
            text = input,
            isComplete = false,
        )
    }

    private fun validateCpf(input: String): PaymentKeyValidation {
        val digits = input.filter(Char::isDigit)
        val isCpf = input.all { it.isDigit() || it in ".- " } && isValidCpf(digits)
        return if (isCpf) {
            PaymentKeyValidation.Valid(digits, PaymentKeyFormat.CPF)
        } else {
            PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_CPF)
        }
    }

    private fun isValidCpf(digits: String): Boolean {
        if (digits.length != CPF_LENGTH || digits.all { it == digits.first() }) return false
        val numbers = digits.map(Char::digitToInt)
        return computeCheckDigit(numbers.take(9)) == numbers[9] && computeCheckDigit(numbers.take(10)) == numbers[10]
    }

    private fun computeCheckDigit(numbers: List<Int>): Int {
        val sum = numbers.mapIndexed { index, number -> number * (numbers.size + 1 - index) }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    private fun validatePhone(input: String): PaymentKeyValidation {
        val digits = extractPhoneDigits(input)
        val isPhone = digits.length == PHONE_LENGTH && digits.take(2).toInt() in DDDS && digits[2] == '9'
        return if (isPhone) {
            PaymentKeyValidation.Valid("+$COUNTRY_CODE$digits", PaymentKeyFormat.PHONE)
        } else {
            PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_PHONE)
        }
    }

    // Why: "+55" in front, as the mask writes it or a pasted number has it, is the country, not the DDD.
    private fun extractPhoneDigits(input: String): String {
        val text = input.trim()
        val digits = text.filter(Char::isDigit)
        return when {
            text.startsWith("+$COUNTRY_CODE") -> digits.removePrefix(COUNTRY_CODE)
            digits.length > PHONE_LENGTH && digits.startsWith(COUNTRY_CODE) -> digits.removePrefix(COUNTRY_CODE)
            else -> digits
        }
    }

    private fun validateEmail(input: String): PaymentKeyValidation {
        val email = input.trim().lowercase()
        return if (email.length <= EMAIL_MAX_LENGTH && EMAIL.matches(email)) {
            PaymentKeyValidation.Valid(email, PaymentKeyFormat.EMAIL)
        } else {
            PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_EMAIL)
        }
    }

    private fun validateRandom(input: String): PaymentKeyValidation {
        val key = input.trim().lowercase()
        if (!UUID.matches(key)) return PaymentKeyValidation.Invalid(PaymentKeyError.INVALID_RANDOM)
        val hex = key.filter { it != '-' }
        val uuid = listOf(hex.take(8), hex.substring(8, 12), hex.substring(12, 16), hex.substring(16, 20), hex.drop(20))
            .joinToString("-")
        return PaymentKeyValidation.Valid(uuid, PaymentKeyFormat.RANDOM)
    }

    // Why: the mask grows as the digits come, so "123456" reads "123.456" while typing.
    private fun formatCpf(digits: String): String = buildString {
        digits.forEachIndexed { index, digit ->
            when (index) {
                3, 6 -> append('.')
                9 -> append('-')
            }
            append(digit)
        }
    }

    private fun formatPhone(digits: String): String {
        if (digits.isEmpty()) return ""
        return buildString {
            append("+$COUNTRY_CODE (")
            digits.forEachIndexed { index, digit ->
                when (index) {
                    2 -> append(") ")
                    7 -> append('-')
                }
                append(digit)
            }
        }
    }

    // Why: what the profile and the settle screen show, from the normalized value.
    fun formatValue(
        type: PaymentKeyType,
        value: String,
    ): String = when (type) {
        PaymentKeyType.CPF -> formatCpf(value)
        PaymentKeyType.PHONE -> formatPhone(extractPhoneDigits(value))
        else -> value
    }
}
