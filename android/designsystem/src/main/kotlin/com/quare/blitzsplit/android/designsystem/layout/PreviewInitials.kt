package com.quare.blitzsplit.android.designsystem.layout

import com.quare.blitzsplit.core.groups.domain.model.Initials

// Why: a preview draws the initials the shared code would hand the screen, which :android:feature:* can't reach.
fun previewInitialsOf(name: String): String = Initials.of(name)
