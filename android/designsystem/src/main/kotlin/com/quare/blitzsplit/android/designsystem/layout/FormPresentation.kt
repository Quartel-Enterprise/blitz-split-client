package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable

/*
 * Why: how a short form over the app opens, which the app turns into its scene: Registrar pagamento (6.5) and Convidar
 * of Contatos (11.4) follow the same postures, so they never open differently on the same device.
 */
enum class FormPresentation {
    // Why: a phone, upright or lying down, where the sheet is at most 640dp wide.
    BottomSheet,

    // Why: 560dp on a tablet and an upright foldable; on a book in the half after the hinge (FoldDialog).
    Dialog,

    // Why: a tabletop: full screen, what to read in the top half, what to tap in the bottom one.
    TabletopHalves,
}

@Composable
fun rememberFormPresentation(): FormPresentation {
    val fold = rememberFoldLayout()
    return when {
        fold.tabletopSplit != null -> FormPresentation.TabletopHalves

        fold.formFactor == FormFactor.Phone || fold.formFactor == FormFactor.PhoneLandscape ->
            FormPresentation.BottomSheet

        else -> FormPresentation.Dialog
    }
}
