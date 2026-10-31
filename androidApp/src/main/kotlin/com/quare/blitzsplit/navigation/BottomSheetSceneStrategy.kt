package com.quare.blitzsplit.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.quare.blitzsplit.android.designsystem.layout.LocalSheetDismissLock

/*
 * Why: Registrar pagamento opens as a bottom sheet over the group on a phone, which stays drawn under it. Dismissing
 * the sheet (dragging it down, tapping the scrim) pops the shared back stack like the back gesture, unless the screen
 * in it holds it open (`SheetDismissLockEffect`).
 */
@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    entry: NavEntry<T>,
    onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        val lock = remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = onBack,
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { value -> value != SheetValue.Hidden || !lock.value },
            ),
        ) {
            CompositionLocalProvider(LocalSheetDismissLock provides lock) {
                entry.Content()
            }
        }
    }
}

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        if (lastEntry.metadata[BottomSheetKey] != true) return null
        return BottomSheetScene(
            key = lastEntry.contentKey,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = lastEntry,
            onBack = onBack,
        )
    }

    companion object {
        object BottomSheetKey : NavMetadataKey<Boolean>

        // Why: marks an entry to draw in a modal bottom sheet over the entry under it.
        val bottomSheet: Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}
