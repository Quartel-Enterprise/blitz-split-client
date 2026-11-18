package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.model.AccountProvider
import io.github.jan.supabase.auth.user.Identity
import kotlin.time.Instant

/*
 * Why: one Supabase user holds an identity per provider, as when the same email is linked to Google and to Apple, and
 * the list comes in the order they were created, so its first one is the first provider ever linked, not the one this
 * person signed in with. Supabase stamps an identity's last_sign_in_at each time it signs in (or is linked), so the
 * latest stamp is the provider of the latest sign in; an identity without a readable stamp comes after the ones that
 * have one, and a tie keeps the listed order. Only Google and Apple count: no other provider has a logo to show.
 */
internal object SignedInProvider {
    fun of(identities: List<Identity>?): AccountProvider? = identities
        .orEmpty()
        .mapNotNull { identity ->
            AccountProvider.fromCodeOrNull(identity.provider)?.let { provider ->
                provider to identity.lastSignInAt?.let(Instant::parseOrNull)
            }
        }.maxWithOrNull(compareBy(nullsFirst<Instant>()) { (_, signedInAt) -> signedInAt })
        ?.first
}
