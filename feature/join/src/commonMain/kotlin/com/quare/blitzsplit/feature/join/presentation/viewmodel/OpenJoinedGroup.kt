package com.quare.blitzsplit.feature.join.presentation.viewmodel

import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: after joining, from the invite or from "Quem é você no grupo?", the group opens over the home, whatever was
 * under the invite: the welcome too, since joining without a profile made one from the member.
 */
internal fun Navigator.openJoinedGroup(
    groupId: String,
    joinedAs: String?,
) {
    replaceAll(Route.Groups)
    navigate(
        Route.Members(
            groupId = groupId,
            joinedAs = joinedAs,
        ),
    )
}
