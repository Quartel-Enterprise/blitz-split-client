package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.ReleaseNotesFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseNotesFeedDao {
    @Query("SELECT * FROM release_notes_feed WHERE language = :language")
    fun observe(language: String): Flow<ReleaseNotesFeedEntity?>

    @Query("SELECT * FROM release_notes_feed WHERE language = :language")
    suspend fun find(language: String): ReleaseNotesFeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReleaseNotesFeedEntity)

    @Query("UPDATE release_notes_feed SET checkedAt = :checkedAt WHERE language = :language")
    suspend fun markChecked(
        language: String,
        checkedAt: Long,
    )
}
