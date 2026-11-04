package com.quare.blitzsplit.core.inappupdate.domain.usecase

// Why: installs what Google Play downloaded, which restarts the app; nothing on the other platforms.
fun interface CompleteUpdateInstall {
    suspend operator fun invoke()
}
