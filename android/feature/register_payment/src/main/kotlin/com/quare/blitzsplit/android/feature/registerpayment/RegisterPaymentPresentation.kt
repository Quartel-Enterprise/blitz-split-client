package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout

// Why: how "Registrar pagamento" (6.5) opens, which the app turns into its scene.
enum class RegisterPaymentPresentation {
    // Why: a phone, upright or lying down, where the 640dp sheet splits in two columns.
    BottomSheet,

    // Why: 560dp on a tablet and an upright foldable; on a book in the half after the hinge (FoldDialog).
    Dialog,

    // Why: a tabletop: full screen, who paid whom and the amount in the top half, how, when and confirm at the bottom.
    TabletopHalves,
}

@Composable
fun rememberRegisterPaymentPresentation(): RegisterPaymentPresentation {
    val fold = rememberFoldLayout()
    return when {
        fold.tabletopSplit != null -> RegisterPaymentPresentation.TabletopHalves

        fold.formFactor == FormFactor.Phone || fold.formFactor == FormFactor.PhoneLandscape ->
            RegisterPaymentPresentation.BottomSheet

        else -> RegisterPaymentPresentation.Dialog
    }
}
