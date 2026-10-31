package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Why: on its own, so the next message replaces this snackbar instead of waiting until it hides.
fun CoroutineScope.showReplacingSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
) {
    snackbarHostState.currentSnackbarData?.dismiss()
    launch { snackbarHostState.showSnackbar(message) }
}

// Why: a screen whose every action is a message shows each one as a snackbar that replaces the one on screen.
@Composable
fun <A> rememberActionSnackbarHostState(
    actions: Flow<A>,
    // Why: the string resource of each action's message.
    messageRes: (A) -> Int,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentMessageRes by rememberUpdatedState(messageRes)
    LaunchedEffect(actions) {
        actions.collect { action ->
            showReplacingSnackbar(snackbarHostState, resources.getString(currentMessageRes(action)))
        }
    }
    return snackbarHostState
}
