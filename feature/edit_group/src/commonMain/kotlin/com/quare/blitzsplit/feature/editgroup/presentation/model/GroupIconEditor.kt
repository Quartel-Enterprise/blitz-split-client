package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: "Ícone do grupo" (6.4d), the sheet Android and iOS open from "Trocar ícone ou foto": the color picked in it,
 * which only reaches the form on "Usar este ícone". Every group is the same rounded square, so there is no shape to pick.
 */
data class GroupIconEditor(
    val color: AvatarColor,
) {
    val colors: List<AvatarColor>
        get() = AvatarColor.entries
}
