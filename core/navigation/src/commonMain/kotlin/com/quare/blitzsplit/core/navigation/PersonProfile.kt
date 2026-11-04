package com.quare.blitzsplit.core.navigation

/*
 * Why: a person tapped wherever a list, a bill or the feed shows them (their name, face or row) opens their profile,
 * the same Contatos opens: personKey is PeopleBalances.keyOf, so a guest and an account resolve as Contatos lists
 * them. This device's own user opens Perfil (the Mac's Ajustes) instead, being nobody's contact.
 */
fun personProfileRouteOf(
    personKey: String,
    isMe: Boolean,
): Route = if (isMe) Route.Profile else Route.ContactProfile(personKey)

fun Navigator.openPersonProfile(
    personKey: String,
    isMe: Boolean,
) {
    navigate(personProfileRouteOf(personKey, isMe))
}
