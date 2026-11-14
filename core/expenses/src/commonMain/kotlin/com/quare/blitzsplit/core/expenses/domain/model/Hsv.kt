package com.quare.blitzsplit.core.expenses.domain.model

// Why: a color as the gradient places it: hue 0 to 360, saturation (left to right) and value (bottom to top) 0 to 1.
data class Hsv(
    val hue: Double,
    val saturation: Double,
    val value: Double,
)
