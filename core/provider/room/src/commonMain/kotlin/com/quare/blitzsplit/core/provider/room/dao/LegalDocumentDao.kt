package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.LegalDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LegalDocumentDao {
    @Query("SELECT * FROM legal_document WHERE page = :page AND language = :language")
    fun observe(
        page: String,
        language: String,
    ): Flow<LegalDocumentEntity?>

    @Query("SELECT * FROM legal_document WHERE page = :page AND language = :language")
    suspend fun find(
        page: String,
        language: String,
    ): LegalDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LegalDocumentEntity)

    @Query("UPDATE legal_document SET checkedAt = :checkedAt WHERE page = :page AND language = :language")
    suspend fun markChecked(
        page: String,
        language: String,
        checkedAt: Long,
    )
}
