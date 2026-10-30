package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Hoje" and "Ontem" are words each UI picks; any other day it writes the date.
enum class RelativeDay {
    TODAY,
    YESTERDAY,
    OTHER,
}
