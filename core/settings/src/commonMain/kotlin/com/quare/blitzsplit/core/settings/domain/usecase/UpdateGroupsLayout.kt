package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.GroupsLayout

fun interface UpdateGroupsLayout {
    suspend operator fun invoke(layout: GroupsLayout)
}
