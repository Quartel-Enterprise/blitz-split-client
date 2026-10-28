package com.quare.blitzsplit.core.sync.connect

import com.quare.blitzsplit.core.model.AccountProvider

// Why: the state of a link on screen, shared by every screen that offers Google or Apple, not a domain model.
data class ProviderConnection(
    val connectingProvider: AccountProvider?,
    val hasFailed: Boolean,
    // Why: "Conectar" and "Conectar Google ou Apple em vez disso" open into one button per provider.
    val isExpanded: Boolean,
)
