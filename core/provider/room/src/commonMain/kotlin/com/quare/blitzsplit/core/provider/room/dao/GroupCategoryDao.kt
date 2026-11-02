package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_CATEGORY_IDS_PER_QUERY = 500

@Dao
interface GroupCategoryDao {
    // Why: the deleted ones too, so a bill of a deleted category still reads as its parent.
    @Query("SELECT * FROM group_category WHERE groupId = :groupId ORDER BY addedAt, id")
    fun observeCategories(groupId: String): Flow<List<GroupCategoryEntity>>

    // Why: every group's categories in one query, for what reads bills across groups.
    @Query("SELECT * FROM group_category ORDER BY groupId, addedAt, id")
    fun observeAllCategories(): Flow<List<GroupCategoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM group_category WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT updatedAt FROM group_category WHERE pendingSync = 1 ORDER BY updatedAt, id")
    fun observePendingUpdatedAts(): Flow<List<Long>>

    @Query(
        "SELECT * FROM group_category WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getPendingCategories(ownerUserId: String): List<GroupCategoryEntity>

    @Query(
        "SELECT id FROM group_category WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedCategoryIds(ownerUserId: String): List<String>

    // Why: a group a sync deleted in the meantime takes no category, which would stay pending with nowhere to go up.
    @Transaction
    suspend fun insertCategoryIntoGroup(entity: GroupCategoryEntity) {
        check(hasGroup(entity.groupId)) { "No group ${entity.groupId}" }
        insertCategory(entity)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM split_group WHERE id = :id)")
    suspend fun hasGroup(id: String): Boolean

    @Insert
    suspend fun insertCategory(entity: GroupCategoryEntity)

    /*
     * Why: "Editar categoria" changes its name, place and icon; its version moves past the stored one even on a clock
     * behind the device that saved it last. Answers how many changed: none once it was deleted meanwhile.
     */
    @Query(
        "UPDATE group_category SET name = :name, parentId = :parentId, icon = :icon, " +
            "updatedAt = MAX(:now, updatedAt + 1), pendingSync = 1 WHERE id = :id AND isDeleted = 0",
    )
    suspend fun updateCategory(
        id: String,
        name: String,
        parentId: String?,
        icon: String?,
        now: Long,
    ): Int

    // Why: "Apagar categoria" leaves a tombstone that the sync takes up, with its subcategories in the same write.
    @Query(
        "UPDATE group_category SET isDeleted = 1, updatedAt = MAX(:now, updatedAt + 1), pendingSync = 1 " +
            "WHERE (id = :id OR parentId = :id) AND isDeleted = 0",
    )
    suspend fun markDeleted(
        id: String,
        now: Long,
    ): Int

    // Why: a category only the server has comes down whole; one the device has follows Last-Write-Wins.
    @Transaction
    suspend fun applyRemoteToGroups(entities: List<GroupCategoryEntity>) {
        entities.filter { hasGroup(it.groupId) }.forEach { entity ->
            insertCategoryIfAbsent(entity)
            updateFromRemote(
                id = entity.id,
                createdBy = entity.createdBy,
                createdByMemberId = entity.createdByMemberId,
                parentId = entity.parentId,
                name = entity.name,
                icon = entity.icon,
                isDeleted = entity.isDeleted,
                addedAt = entity.addedAt,
                remoteUpdatedAt = entity.updatedAt,
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoryIfAbsent(entity: GroupCategoryEntity)

    // Why: Last-Write-Wins as the server keeps it, a newer server row winning even over a change not pushed yet.
    @Query(
        "UPDATE group_category SET createdBy = :createdBy, createdByMemberId = :createdByMemberId, " +
            "parentId = :parentId, name = :name, icon = :icon, isDeleted = :isDeleted, addedAt = :addedAt, " +
            "updatedAt = :remoteUpdatedAt, pendingSync = 0 WHERE id = :id AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateFromRemote(
        id: String,
        createdBy: String?,
        createdByMemberId: String?,
        parentId: String?,
        name: String,
        icon: String?,
        isDeleted: Boolean,
        addedAt: Long,
        remoteUpdatedAt: Long,
    )

    // Why: every category a push took up in one transaction, so the observers wake once.
    @Transaction
    suspend fun markAllSynced(pushed: List<GroupCategoryEntity>) {
        pushed.forEach {
            markSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
                createdBy = it.createdBy,
            )
        }
    }

    // Why: a change made while the push was in flight keeps the category pending.
    @Query(
        "UPDATE group_category SET pendingSync = 0, createdBy = :createdBy " +
            "WHERE id = :id AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
        createdBy: String?,
    )

    // Why: the server answers every category of the user's groups, so a synced one missing there was removed there.
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCategoryIds: Set<String>,
    ) {
        getSyncedCategoryIds(ownerUserId)
            .filterNot { it in serverCategoryIds }
            .chunked(MAX_CATEGORY_IDS_PER_QUERY)
            .forEach { deleteCategories(it) }
    }

    @Query("DELETE FROM group_category WHERE id IN (:ids)")
    suspend fun deleteCategories(ids: List<String>)
}
