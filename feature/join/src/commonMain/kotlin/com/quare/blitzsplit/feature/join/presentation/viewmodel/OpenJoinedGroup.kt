package com.quare.blitzsplit.feature.join.presentation.viewmodel

import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: after joining, from the invite or from "Quem é você no grupo?", the group opens over the home's Grupos, whatever
 * was under the invite: the welcome too, since joining without a profile made one from the member. "Membros" says
 * "Você entrou como Bruno"; with no name to say, the group's own screen opens.
 */
internal fun Navigator.openJoinedGroup(
    groupId: String,
    joinedAs: String?,
) {
    replaceAll(Route.Home)
    navigate(Route.Groups)
    navigate(
        if (joinedAs == null) {
            Route.GroupDetail(groupId)
        } else {
            Route.Members(
                groupId = groupId,
                joinedAs = joinedAs,
            )
        },
    )
}
