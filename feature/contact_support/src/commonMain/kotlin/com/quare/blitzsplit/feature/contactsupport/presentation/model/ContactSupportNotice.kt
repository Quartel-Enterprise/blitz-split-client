package com.quare.blitzsplit.feature.contactsupport.presentation.model

// Why: the line under the buttons, which each UI says in its own words.
enum class ContactSupportNotice {
    EMAIL_COPIED,

    // Why: no app took the mailto link, so the address went to the clipboard instead.
    NO_EMAIL_APP,
}
