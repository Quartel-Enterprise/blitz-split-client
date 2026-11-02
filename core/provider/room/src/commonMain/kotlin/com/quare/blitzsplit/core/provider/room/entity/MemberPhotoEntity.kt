package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: the profile photo of someone who joined a group, by account, so the faces of every group they are in show it
 * offline too. Only accounts with a photo have a row; each group download replaces them all with the server's.
 */
@Entity(tableName = "member_photo")
data class MemberPhotoEntity(
    @PrimaryKey
    val userId: String,
    val photoUrl: String,
)
