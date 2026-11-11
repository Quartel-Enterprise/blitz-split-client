package com.quare.blitzsplit.core.utils

/*
 * Why: the code of a link people share, made on the device so the link exists offline (a group's invite link,
 * blitzsplit.com/g/<code>). Injected so tests know which code comes out.
 */
fun interface RandomCode {
    // Why: `length` characters of [A-Za-z0-9], the alphabet every link of the app is matched with.
    operator fun invoke(length: Int): String
}
