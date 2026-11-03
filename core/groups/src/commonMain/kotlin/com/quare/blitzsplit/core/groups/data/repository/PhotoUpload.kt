package com.quare.blitzsplit.core.groups.data.repository

// Why: how a group's photo upload ended; a failure that may pass is the Result's.
internal enum class PhotoUpload {
    UPLOADED,
    REFUSED,
}
