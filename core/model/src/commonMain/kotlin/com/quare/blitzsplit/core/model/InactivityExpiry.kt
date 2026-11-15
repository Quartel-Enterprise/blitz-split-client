package com.quare.blitzsplit.core.model

/*
 * Why: the server deletes an account without Google or Apple that no device synced for 30 days. A device that comes
 * back to one is erased, then tells the person on the welcome.
 */
enum class InactivityExpiry(
    val code: String,
) {
    // Why: the session was refused and the device still holds the deleted account's copy.
    PENDING("pending"),

    // Why: the device was erased, and the welcome hasn't told the person yet.
    NOTICE("notice"),
    ;

    companion object {
        // Why: the days without a sync after which the server deletes the account, as the screens tell.
        const val IDLE_DAYS = 30

        fun fromCodeOrNull(code: String?): InactivityExpiry? = entries.firstOrNull { it.code == code }
    }
}
