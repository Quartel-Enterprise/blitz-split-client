package com.quare.blitzsplit.core.utils

/*
 * Why: the tag of the language the device itself is in ("tr", "pt-BR"), null when the platform doesn't say. What reads
 * in a language the app has no strings for (the legal texts) asks for it while Profile › Language follows the system.
 */
fun interface DeviceLanguage {
    operator fun invoke(): String?
}
