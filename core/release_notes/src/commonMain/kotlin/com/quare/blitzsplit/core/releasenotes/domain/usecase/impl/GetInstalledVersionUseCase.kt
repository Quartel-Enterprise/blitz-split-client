package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetInstalledVersion

class GetInstalledVersionUseCase(
    // Why: version.properties' versionName as the build held it, "3.1.0" or a beta's "3.1.0-beta_2".
    private val versionName: String,
) : GetInstalledVersion {
    override fun invoke(): AppVersion? = AppVersion.parseOrNull(versionName)
}
