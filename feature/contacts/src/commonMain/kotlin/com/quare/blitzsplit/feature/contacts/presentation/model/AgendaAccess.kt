package com.quare.blitzsplit.feature.contacts.presentation.model

// Why: what the platform says about the agenda; the shared code only decides what each one shows.
enum class AgendaAccess {
    // Why: not read yet.
    UNKNOWN,

    // Why: never asked: the screen explains why before the system asks (11.3b).
    NOT_ASKED,

    GRANTED,

    // Why: iOS's limited access: only the contacts the person picked show.
    LIMITED,

    // Why: refused: the screen leads to the device's settings and offers the link (11.3c).
    DENIED,

    // Why: the web, where the browser doesn't read the agenda.
    UNAVAILABLE,
}
