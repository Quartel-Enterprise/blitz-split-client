package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion

// Why: the version this app is, null when its name isn't one the release notes order (a local build's).
fun interface GetInstalledVersion {
    operator fun invoke(): AppVersion?
}
