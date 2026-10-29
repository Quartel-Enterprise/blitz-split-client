package com.quare.blitzsplit.core.profile.domain.model

// Why: a Pix email and a Lightning Address are both name@domain, so one grammar serves both rules.
internal val EMAIL_ADDRESS = Regex("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$")
