package com.quare.blitzsplit.core.sync.domain.usecase

fun interface ReportAppForeground {
    operator fun invoke(isForeground: Boolean)
}
