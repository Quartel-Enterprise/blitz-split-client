package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout

// Why: how "Convidar" (11.4) opens, which the app turns into its scene.
enum class ContactInvitePresentation {
    // Why: a phone, upright or lying down (centered, at most 640dp wide).
    BottomSheet,

    // Why: 560dp on a tablet and an upright foldable; on a book in the half after the hinge (FoldDialog).
    Dialog,

    // Why: a tabletop: the QR and the link in the top half, the groups and the buttons in the bottom one.
    TabletopHalves,
}

@Composable
fun rememberContactInvitePresentation(): ContactInvitePresentation {
    val fold = rememberFoldLayout()
    return when {
        fold.tabletopSplit != null -> ContactInvitePresentation.TabletopHalves

        fold.formFactor == FormFactor.Phone || fold.formFactor == FormFactor.PhoneLandscape ->
            ContactInvitePresentation.BottomSheet

        else -> ContactInvitePresentation.Dialog
    }
}
