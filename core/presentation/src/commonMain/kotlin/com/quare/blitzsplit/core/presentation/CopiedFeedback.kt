package com.quare.blitzsplit.core.presentation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Why: how long "Copiado" stays after "Copiar", the same on every screen that copies a link.
object CopiedFeedback {
    val duration: Duration = 2.seconds
}
