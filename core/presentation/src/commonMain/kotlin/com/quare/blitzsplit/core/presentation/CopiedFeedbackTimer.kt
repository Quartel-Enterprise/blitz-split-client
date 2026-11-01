package com.quare.blitzsplit.core.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Why: "Copiado" after "Copiar" for CopiedFeedback.duration; a second tap keeps it that long from that tap.
class CopiedFeedbackTimer(
    private val scope: CoroutineScope,
    private val onCopiedChanged: (Boolean) -> Unit,
) {
    private var reset: Job? = null

    fun start() {
        onCopiedChanged(true)
        reset?.cancel()
        reset = scope.launch {
            delay(CopiedFeedback.duration)
            onCopiedChanged(false)
        }
    }
}
