package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.OpenLegalPage

class OpenLegalPageUseCase(
    private val navigator: Navigator,
    private val clientPlatform: ClientPlatform,
) : OpenLegalPage {
    override fun invoke(
        page: LegalPage,
        openUrl: (String) -> Unit,
    ) {
        if (clientPlatform == ClientPlatform.WEB) {
            openUrl(page.siteUrl)
        } else {
            navigator.navigate(
                Route.LegalDocument(
                    page = page,
                    anchor = null,
                ),
            )
        }
    }
}
