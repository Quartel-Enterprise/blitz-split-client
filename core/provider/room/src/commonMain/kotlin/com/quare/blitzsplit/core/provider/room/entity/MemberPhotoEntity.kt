package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: what the faces and a contact's profile need of someone who joined a group, by account, so they show offline
 * too: the profile photo and the code of their profile link (blitzsplit.com/u/<code>, the contact's "Share profile").
 * Only accounts with either have a row; each group download replaces them all with the server's.
 */
@Entity(tableName = "member_photo")
data class MemberPhotoEntity(
    @PrimaryKey
    val userId: String,
    // Why: null for an account with no photo, whose face keeps the initials.
    val photoUrl: String?,
    // Why: the server's profiles.share_code, which every account has; null only until a sync brings it.
    val shareCode: String?,
)
