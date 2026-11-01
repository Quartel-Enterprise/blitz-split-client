package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/*
 * Why: the pane beside a list (Grupos, Contatos) before something is picked, or once back closed it: the canvas'
 * shapes, with no words of their own.
 */
@Composable
fun DetailPlaceholderPane(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(contentAlignment = Alignment.Center) {
            EmptyGroupsImage(scale = 1.25f)
        }
    }
}
