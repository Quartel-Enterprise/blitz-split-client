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
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement

/*
 * Why: Navigation 3's dialog scene, drawn by FoldDialog: centered as before wherever a flow opens a dialog today,
 * and in the end half of a book (top half of a tabletop) once a flow opens one there, with the screen under it in
 * the other half. As Navigation 3's own scene does, the entry in the dialog gets a lifecycle of its own.
 */
private class FoldDialogScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val properties: DialogProperties,
    private val isTopHalfOnTabletop: Boolean,
    onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        FoldDialog(
            onDismissRequest = onBack,
            properties = properties,
            placement = rememberDialogPlacement(isTopHalfOnTabletop = isTopHalfOnTabletop),
        ) {
            CompositionLocalProvider(LocalLifecycleOwner provides rememberLifecycleOwner()) {
                entry.Content()
            }
        }
    }

    // Why: a scene calculated again for the same entries is the same scene, as Navigation 3's own dialog scene says.
    override fun equals(other: Any?): Boolean = other is FoldDialogScene<*> &&
        key == other.key &&
        previousEntries == other.previousEntries &&
        overlaidEntries == other.overlaidEntries &&
        entry == other.entry &&
        properties == other.properties &&
        isTopHalfOnTabletop == other.isTopHalfOnTabletop

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        result = 31 * result + entry.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + isTopHalfOnTabletop.hashCode()
        return result
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
            isTopHalfOnTabletop = lastEntry.metadata[TopHalfOnTabletopKey] ?: false,
            onBack = onBack,
        )
    }

    companion object {
        object FoldDialogKey : NavMetadataKey<DialogProperties>

        object TopHalfOnTabletopKey : NavMetadataKey<Boolean>

        /*
         * Why: marks an entry to draw in a dialog over the entry under it, placed off the hinge: the canvas' default
         * per posture, or a tabletop's top half even lying down where the flow's boards put it there (flow 2).
         */
        fun dialogMetadataOf(
            properties: DialogProperties,
            isTopHalfOnTabletop: Boolean = false,
        ): Map<String, Any> = metadata {
            put(FoldDialogKey, properties)
            put(TopHalfOnTabletopKey, isTopHalfOnTabletop)
        }
    }
}
