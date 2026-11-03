package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupShape

/*
 * Why: "Ícone do grupo" (6.4d), the sheet Android and iOS open from "Trocar ícone ou foto": the shape and color
 * picked in it, which only reach the form on "Usar este ícone". Android picks both, iOS only the color.
 */
data class GroupIconEditor(
    val color: AvatarColor,
    val shape: GroupShape,
) {
    val colors: List<AvatarColor>
        get() = AvatarColor.entries

    val shapes: List<GroupShape>
        get() = GroupShape.entries
}
