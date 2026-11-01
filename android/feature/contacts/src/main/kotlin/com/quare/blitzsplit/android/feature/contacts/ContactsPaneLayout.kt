package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberListDetailLayout

private val railWidth = 80.dp
private val phoneLandscapeListWidth = 380.dp

/*
 * Why: where Contatos draws the person picked (or the agenda) beside the list: a tablet lying down and an unfolded book
 * as Saldos does (520dp with the rail, the half before the hinge), and, unlike Saldos, a phone lying down too, its
 * list 380dp after the rail, as the canvas' landscape boards draw it.
 */
@Immutable
internal data class ContactsPaneLayout(
    // Why: the list after the rail.
    val listWidth: Dp,
    val paneSpacing: Dp,
    val paneEndPadding: Dp,
)

// Why: null while the window draws one column, where the profile and the agenda open over the list.
@Composable
internal fun rememberContactsPaneLayout(): ContactsPaneLayout? {
    val listDetail = rememberListDetailLayout()
    val formFactor = rememberFoldLayout().formFactor
    return remember(listDetail, formFactor) {
        when {
            listDetail != null -> ContactsPaneLayout(
                listWidth = listDetail.listPaneWidth - railWidth,
                paneSpacing = listDetail.paneSpacing,
                paneEndPadding = if (listDetail.isBook) 16.dp else 32.dp,
            )

            formFactor == FormFactor.PhoneLandscape -> ContactsPaneLayout(
                listWidth = phoneLandscapeListWidth,
                paneSpacing = 8.dp,
                paneEndPadding = 16.dp,
            )

            else -> null
        }
    }
}
