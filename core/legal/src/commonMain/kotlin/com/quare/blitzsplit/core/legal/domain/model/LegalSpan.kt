package com.quare.blitzsplit.core.legal.domain.model

data class LegalSpan(
    val text: String,
    val isBold: Boolean,
    val link: LegalLink?,
)
