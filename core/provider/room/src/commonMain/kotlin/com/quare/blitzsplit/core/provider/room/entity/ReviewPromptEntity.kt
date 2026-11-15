package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: what decides when this device asks for a review in the store. It belongs to the device, so it is never synced
 * and outlives a sign out, or signing in again would ask again.
 */
@Entity(tableName = "review_prompt")
data class ReviewPromptEntity(
    @PrimaryKey
    val id: String,
    // Why: the bills created on this device, since the third one is a moment to ask at.
    val billsCreated: Int,
    // Why: the first moment worth asking at (epoch millis), which the days of grace count from; null until there is one.
    val firstEligibleAt: Long?,
    // Why: when the review was last asked for (epoch millis); null until asked once.
    val lastPromptedAt: Long?,
    // Why: the app version that last asked, so each version asks once at most; null until asked once.
    val lastPromptedVersion: String?,
)
