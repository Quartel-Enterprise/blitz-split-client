package com.quare.blitzsplit

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.BillLinkNotice

/*
 * Why: a comment's link that can't open its bill says why over wherever the app is ("Você não está neste grupo",
 * "Sem internet"), with "Ir para o início"; a book draws it in its right half, as the app's other dialogs.
 */
@Composable
internal fun BillLinkNoticeDialog(appViewModel: AppViewModel) {
    val noticeState by appViewModel.billLinkNotice.collectAsStateWithLifecycle()
    val notice = noticeState.notice ?: return
    FoldAlertDialog(
        onDismissRequest = appViewModel::onBillLinkNoticeDismissed,
        title = stringResource(
            when (notice) {
                BillLinkNotice.NOT_IN_GROUP -> R.string.bill_link_not_in_group_title
                BillLinkNotice.OFFLINE -> R.string.bill_link_offline_title
            },
        ),
        text = {
            Text(
                stringResource(
                    when (notice) {
                        BillLinkNotice.NOT_IN_GROUP -> R.string.bill_link_not_in_group_message
                        BillLinkNotice.OFFLINE -> R.string.bill_link_offline_message
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = appViewModel::onBillLinkNoticeDismissed) {
                Text(stringResource(R.string.bill_link_go_home))
            }
        },
        placement = rememberDialogPlacement(),
    )
}
