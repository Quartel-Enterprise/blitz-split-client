package com.quare.blitzsplit.core.groups.data.datasource

// Why: a group's photo in the `group-photos` bucket, one object per group overwritten by each new photo.
interface GroupPhotoRemoteDataSource {
    // Why: known before the upload, so the group's row and its photo go up in one round whatever comes first.
    fun urlOf(
        groupId: String,
        version: Long,
    ): String

    // Why: false when Storage refuses this photo for good, so it is never tried again.
    suspend fun upload(
        groupId: String,
        jpeg: ByteArray,
    ): Boolean
}
