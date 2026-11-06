package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.AppLanguage

fun interface UpdateAppLanguage {
    operator fun invoke(language: AppLanguage)
}
