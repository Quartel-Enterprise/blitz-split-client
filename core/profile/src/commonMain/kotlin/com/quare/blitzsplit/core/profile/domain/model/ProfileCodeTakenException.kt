package com.quare.blitzsplit.core.profile.domain.model

// Why: the server refused the profile's share code this device made, since another profile has it.
class ProfileCodeTakenException : IllegalStateException("Another profile already has this share code")
