package com.quare.blitzsplit.feature.join.presentation.viewmodel

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: after joining, from the invite or from "Quem é você no grupo?", the group's Despesas opens over the home's
 * Grupos (3.3), whatever was under the invite: the welcome too, since joining without a profile made one from the
 * member. It says "Você entrou como Bruno" while there is a name to say.
 */
internal fun Navigator.openJoinedGroup(
    groupId: String,
    joinedAs: String?,
) {
    replaceAll(Route.Home)
    navigate(Route.Groups)
    navigate(
        Route.GroupDetail(
            groupId = groupId,
            joinedAs = joinedAs,
            tab = GroupTab.EXPENSES,
        ),
    )
}
