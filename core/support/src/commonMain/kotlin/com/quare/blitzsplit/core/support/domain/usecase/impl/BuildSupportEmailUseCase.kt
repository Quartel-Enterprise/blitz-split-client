package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.support.domain.model.SupportAccount
import com.quare.blitzsplit.core.support.domain.model.SupportContact
import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportEmail
import com.quare.blitzsplit.core.support.domain.model.SupportPlatform
import com.quare.blitzsplit.core.support.domain.usecase.BuildSupportEmail

/*
 * Why: the subject and the diagnostics are for whoever answers the email, so they are in English whatever the app's
 * language; the person writes above the rule, in their own.
 */
class BuildSupportEmailUseCase : BuildSupportEmail {
    override fun invoke(diagnostics: SupportDiagnostics): SupportEmail {
        val subject = "$SUBJECT (${diagnostics.platform.label} ${diagnostics.appVersion})"
        val body = diagnostics.toBody()
        return SupportEmail(
            mailtoLink = "mailto:${SupportContact.EMAIL}" +
                "?subject=${subject.encodeForMailto()}" +
                "&body=${body.encodeForMailto()}",
            subject = subject,
            body = body,
        )
    }

    // Why: two empty lines first, where the person writes, then the rule the diagnostics sit under.
    private fun SupportDiagnostics.toBody(): String = buildString {
        append("\n\n---\n")
        append("App version: $appVersion\n")
        append("Platform: ${platform.label}\n")
        // Why: a pick of System alone would not say which language the person reads the answer in.
        append("Language: ${language.tag ?: "System ($spokenLanguage)"}\n")
        append("Account: ${account.label}")
        profileLink?.let { append("\nProfile: $it") }
    }

    private val SupportPlatform.label: String
        get() = when (this) {
            SupportPlatform.ANDROID -> "Android"
            SupportPlatform.IOS -> "iOS"
            SupportPlatform.MACOS -> "macOS"
            SupportPlatform.WEB -> "Web"
        }

    private val SupportAccount.label: String
        get() = when (this) {
            SupportAccount.LOCAL_ONLY -> "Local only"
            SupportAccount.ANONYMOUS -> "Anonymous"
            SupportAccount.GOOGLE -> "Google"
            SupportAccount.APPLE -> "Apple"
        }

    /*
     * Why: RFC 6068 takes a mailto's subject and body percent-encoded as UTF-8, a space as %20 (a "+" would reach
     * the message as a plus sign) and a line break as %0D%0A.
     */
    private fun String.encodeForMailto(): String = buildString {
        this@encodeForMailto.replace("\n", "\r\n").encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and BYTE_MASK
            val char = code.toChar()
            if (char in UNRESERVED) {
                append(char)
            } else {
                append('%')
                append(HEX_DIGITS[code shr HEX_SHIFT])
                append(HEX_DIGITS[code and LOW_NIBBLE])
            }
        }
    }

    private companion object {
        const val SUBJECT = "Blitz Split support"
        const val BYTE_MASK = 0xFF
        const val HEX_SHIFT = 4
        const val LOW_NIBBLE = 0x0F
        const val HEX_DIGITS = "0123456789ABCDEF"
        const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
    }
}
