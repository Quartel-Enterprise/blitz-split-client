package com.quare.blitzsplit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog

/*
 * Why: Navigation 3's dialog scene, drawn by FoldDialog: centered as before wherever a flow opens a dialog today,
 * and in the end half of a book (top half of a tabletop) once a flow opens one there, with the screen under it in
 * the other half. As Navigation 3's own scene does, the entry in the dialog gets a lifecycle of its own.
 */
private class FoldDialogScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    entry: NavEntry<T>,
    properties: DialogProperties,
    onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        FoldDialog(
            onDismissRequest = onBack,
            properties = properties,
        ) {
            CompositionLocalProvider(LocalLifecycleOwner provides rememberLifecycleOwner()) {
                entry.Content()
            }
        }
    }
}

class FoldDialogSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val properties = lastEntry.metadata[FoldDialogKey] ?: return null
        return FoldDialogScene(
            key = lastEntry.contentKey,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = lastEntry,
            properties = properties,
            onBack = onBack,
        )
    }

    companion object {
        object FoldDialogKey : NavMetadataKey<DialogProperties>

        // Why: marks an entry to draw in a dialog over the entry under it, placed off the hinge.
        fun dialogMetadataOf(properties: DialogProperties): Map<String, Any> =
            metadata { put(FoldDialogKey, properties) }
    }
}
