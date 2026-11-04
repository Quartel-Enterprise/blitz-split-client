package com.quare.blitzsplit.core.navigation

/*
 * Why: a person tapped wherever a list, a bill or the feed shows them (their name, face or row) opens their profile,
 * the same Contatos opens: personKey is PeopleBalances.keyOf, so a guest and an account resolve as Contatos lists
 * them. This device's own user opens Perfil (the Mac's Ajustes) instead, being nobody's contact; anyone else without
 * a key (no longer in the group) has none to open.
 */
fun personProfileRouteOf(
    personKey: String?,
    isMe: Boolean,
): Route? = when {
    isMe -> Route.Profile
    personKey != null -> Route.ContactProfile(personKey)
    else -> null
}

fun Navigator.openPersonProfile(
    personKey: String?,
    isMe: Boolean,
) {
    personProfileRouteOf(personKey, isMe)?.let(::navigate)
}
