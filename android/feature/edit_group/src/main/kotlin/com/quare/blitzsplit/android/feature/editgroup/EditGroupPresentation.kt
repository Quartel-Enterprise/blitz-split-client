package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout

private val defaultSplitDialogFormFactors = setOf(
    FormFactor.TabletPortrait,
    FormFactor.TabletLandscape,
    FormFactor.Book,
    FormFactor.Tabletop,
)

/*
 * Why: the canvas draws Editar grupo as a dialog over the group on anything wider than a phone: 560dp on a tablet and
 * a foldable upright, in a book's right half (a tabletop drawn like the book), the group under the scrim. A phone opens
 * it full screen, in two columns lying down.
 */
@Composable
fun rememberUsesEditGroupDialog(): Boolean {
    val formFactor = rememberFoldLayout().formFactor
    return formFactor != FormFactor.Phone && formFactor != FormFactor.PhoneLandscape
}

/*
 * Why: "Divisão padrão" (6.7) is a dialog over the group on a tablet (920dp in two columns lying down, 560dp upright)
 * and in a book's right half (a tabletop drawn like the book); a phone, lying down too, and a foldable upright open it
 * full screen, as the canvas draws it.
 */
@Composable
fun rememberUsesDefaultSplitDialog(): Boolean = rememberFoldLayout().formFactor in defaultSplitDialogFormFactors
