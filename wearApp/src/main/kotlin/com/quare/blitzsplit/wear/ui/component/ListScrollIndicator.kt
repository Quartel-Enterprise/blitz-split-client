package com.quare.blitzsplit.wear.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.ScrollIndicator

// Why: hidden while the system captures a long screenshot, which would draw it on every part of the capture.
@Composable
fun BoxScope.ListScrollIndicator(state: TransformingLazyColumnState) {
    if (!LocalScrollCaptureInProgress.current) {
        ScrollIndicator(
            state = state,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
