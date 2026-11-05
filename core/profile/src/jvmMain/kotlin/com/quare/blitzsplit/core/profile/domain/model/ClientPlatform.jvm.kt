package com.quare.blitzsplit.core.profile.domain.model

// Why: the jvm target only runs the tests, which read it as the web, one of the platforms the invite shows on.
actual val currentClientPlatform: ClientPlatform = ClientPlatform.WEB
