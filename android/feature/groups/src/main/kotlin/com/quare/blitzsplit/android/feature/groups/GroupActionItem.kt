package com.quare.blitzsplit.android.feature.groups

import androidx.compose.ui.graphics.vector.ImageVector
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent

// Why: one of "Convidar pessoas", "Sair do grupo" and "Apagar grupo", the same in the ⋮ menu and the long press sheet.
internal data class GroupActionItem(
    val label: String,
    // Why: "Link ou QR do grupo", "Acerte os R$ 98,00 antes", "Precisa de internet"; null when nothing needs saying.
    val hint: String?,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val isDestructive: Boolean,
    val event: GroupsUiEvent,
)
