package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: "Como funciona" (13.2) has no ViewModel, only words and the route's currency; back goes to whichever opened it,
 * Editar grupo or the group's Saldos, full screen at every size.
 */
@Composable
fun SimplifyDebtsInfoScreen(
    route: Route.SimplifyDebtsInfo,
    onBack: () -> Unit,
) {
    SimplifyDebtsInfoContent(
        currency = route.currency,
        onBack = onBack,
    )
}

@DevicePreviews
@Composable
private fun SimplifyDebtsInfoPreview() {
    BlitzSplitTheme {
        SimplifyDebtsInfoContent(
            currency = GroupCurrency.BRL,
            onBack = {},
        )
    }
}

@DevicePreviews
@Composable
private fun SimplifyDebtsInfoYenPreview() {
    BlitzSplitTheme {
        SimplifyDebtsInfoContent(
            currency = GroupCurrency.JPY,
            onBack = {},
        )
    }
}
