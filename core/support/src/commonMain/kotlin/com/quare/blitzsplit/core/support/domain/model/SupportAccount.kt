package com.quare.blitzsplit.core.support.domain.model

// Why: how the install is known to the server, which tells support where the person's data lives.
enum class SupportAccount {
    LOCAL_ONLY,
    ANONYMOUS,
    GOOGLE,
    APPLE,
}
