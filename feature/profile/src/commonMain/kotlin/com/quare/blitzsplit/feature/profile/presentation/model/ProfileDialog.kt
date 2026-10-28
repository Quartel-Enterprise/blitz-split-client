package com.quare.blitzsplit.feature.profile.presentation.model

enum class ProfileDialog {
    // Why: "Quem vê seu perfil", behind the info button next to the name.
    VISIBILITY_INFO,

    // Why: with Google or Apple and nothing queued, signing out only drops this device's copy.
    SIGN_OUT,

    // Why: with Google or Apple and changes still queued, the copy stays until they are up.
    SIGN_OUT_PENDING,

    // Why: without an account the profile only lives here, and the recovery code is the way back.
    ERASE_PROFILE,
}
