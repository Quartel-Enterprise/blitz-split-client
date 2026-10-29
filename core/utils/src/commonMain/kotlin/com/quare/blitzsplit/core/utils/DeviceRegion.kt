package com.quare.blitzsplit.core.utils

// Why: the ISO 3166 region of the device's language ("BR" for pt-BR), null when it has none. Each platform reads it.
fun interface DeviceRegion {
    operator fun invoke(): String?
}
