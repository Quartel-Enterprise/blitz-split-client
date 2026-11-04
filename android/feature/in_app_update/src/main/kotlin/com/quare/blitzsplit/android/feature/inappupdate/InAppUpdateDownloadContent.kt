package com.quare.blitzsplit.android.feature.inappupdate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateDownloadUiState

private const val PERCENT_MAX = 100f
private val maxCardWidth = 560.dp

@Composable
fun InAppUpdateDownloadContent(
    state: InAppUpdateDownloadUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        state.downloadPercent?.let { percent -> UpdateProgressCard(percent = percent) }
        SnackbarHost(snackbarHostState)
    }
}

/*
 * Why: drawn with a background rather than a Surface, which would take every tap under it: the bottom bar and "Rachar
 * conta" stay usable for the whole download.
 */
@Composable
private fun UpdateProgressCard(percent: Int) {
    Column(
        modifier = Modifier
            .widthIn(max = maxCardWidth)
            .fillMaxWidth()
            .padding(16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.inverseOnSurface) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.in_app_update_downloading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        LinearProgressIndicator(
            progress = { percent / PERCENT_MAX },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.inversePrimary,
        )
    }
}

@DevicePreviews
@Composable
private fun InAppUpdateDownloadContentPreview() {
    BlitzSplitTheme {
        InAppUpdateDownloadContent(
            state = InAppUpdateDownloadUiState(downloadPercent = 42),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
