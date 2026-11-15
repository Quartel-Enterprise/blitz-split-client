package com.quare.blitzsplit.android.feature.contactsupport

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.copyText
import com.quare.blitzsplit.android.platform.openEmail
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiAction
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiEvent
import com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel.ContactSupportViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: "Contact support" (Route.ContactSupport), from Profile's "Support and feedback": a bottom sheet on a phone and
 * a dialog wider, as short as "Who paid whom?".
 */
@Composable
fun ContactSupportScreen(
    isDialog: Boolean,
    viewModel: ContactSupportViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is ContactSupportUiAction.CopyToClipboard -> context.copyText(action.email)

                is ContactSupportUiAction.OpenEmail -> {
                    val message = action.message
                    if (!context.openEmail(message.mailtoLink, message.subject, message.body)) {
                        viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailFailed)
                    }
                }
            }
        }
    }
    BackHandler { viewModel.onEvent(ContactSupportUiEvent.OnDismissed) }
    ContactSupportContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
}
