package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.ReviewPromptEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID

@Dao
interface ReviewPromptDao {
    @Query("SELECT * FROM review_prompt WHERE id = '$SINGLE_ROW_ID'")
    suspend fun get(): ReviewPromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReviewPromptEntity)

    // Why: the read and the write in one transaction, so two bills saved together are both counted.
    @Transaction
    suspend fun update(
        empty: ReviewPromptEntity,
        transform: (ReviewPromptEntity) -> ReviewPromptEntity,
    ): ReviewPromptEntity {
        val updated = transform(get() ?: empty)
        upsert(updated)
        return updated
    }
}
