package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.RemotePull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/*
 * Why: what the device brings down short of a full sync, one group at a time: a first download's groups (5.1d) and
 * what real time heard (RemotePull). Run under SyncCoordinator's lock, like the sync it stands in for.
 */
class RemotePuller(
    private val profileRepository: ProfileRepository,
    private val paymentKeyRepository: PaymentKeyRepository,
    private val groupRepository: GroupRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val billCommentRepository: BillCommentRepository,
    private val billAttachmentRepository: BillAttachmentRepository,
    private val activityRepository: ActivityRepository,
) {
    /*
     * Why: a group's rows are all read first, then written in Room's foreign key order with no round trip in between,
     * so no screen shows a group with its bills and without its payments. Nothing leaves the device, which only the
     * full download decides.
     */
    suspend fun downloadGroup(
        userId: String,
        groupId: String,
    ) {
        val writes = coroutineScope {
            listOf(
                async { groupRepository.fetchGroupDownload(userId, groupId) },
                async { categoryRepository.fetchGroupCategories(groupId) },
                async { expenseRepository.fetchGroupExpenses(groupId) },
                async { settlementRepository.fetchGroupSettlements(groupId) },
            ).awaitAll()
        }.map { it.getOrThrow() }
        withContext(NonCancellable) { writes.forEach { write -> write() } }
    }

    // Why: the groups side by side, each writing its rows together; Atividade's new events last, with what they tell of.
    suspend fun pull(
        userId: String,
        pull: RemotePull,
    ) {
        if (pull.pullsOwnProfile) profileRepository.downloadProfile(userId).getOrThrow()
        coroutineScope { pull.groupIds.map { groupId -> async { downloadGroup(userId, groupId) } }.awaitAll() }
        if (pull.pullsComments) billCommentRepository.download(userId).getOrThrow()
        if (pull.pullsAttachments) billAttachmentRepository.download(userId).getOrThrow()
        if (pull.pullsMemberPhotos) groupRepository.downloadMemberPhotos().getOrThrow()
        if (pull.pullsMemberKeys) paymentKeyRepository.downloadMemberKeys(userId).getOrThrow()
        if (pull.pullsActivity) activityRepository.downloadActivity(userId).getOrThrow()
    }
}
