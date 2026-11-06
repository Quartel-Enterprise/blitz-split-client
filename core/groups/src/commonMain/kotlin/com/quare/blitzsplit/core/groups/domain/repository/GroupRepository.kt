package com.quare.blitzsplit.core.groups.domain.repository

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsChange
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    // Why: the most recently changed first.
    fun observeGroups(): Flow<List<Group>>

    // Why: whether the device has a group, settled ones included, without reading the groups and their members.
    fun observeHasGroups(): Flow<Boolean>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: the group and its members in one write, so nobody sees the group without them.
    suspend fun createGroup(
        group: Group,
        createdAt: Long,
    ): Result<Unit>

    // Why: "Editar grupo", by any member; the members stay as they are.
    suspend fun updateGroup(
        groupId: String,
        name: String,
        type: GroupType,
        color: AvatarColor,
        // Why: null keeps the stored one, which may be none (a group made before shapes) or a newer app's.
        shape: GroupShape?,
        // Why: a Pending one goes up with the next sync; null takes the photo off.
        photo: ProfilePhoto?,
        currency: GroupCurrency,
        updatedAt: Long,
    ): Result<Unit>

    /*
     * Why: "Divisão padrão" (6.7), by any member, offline too: only the split changes, null back to splitting equally
     * among everyone. The incomes "Pela renda" was typed with, by member id in minor units, stay on this device.
     */
    suspend fun updateDefaultSplit(
        groupId: String,
        defaultSplit: GroupDefaultSplit?,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ): Result<Unit>

    /*
     * Why: "Simplificar dívidas" (13.1), by any member, offline too, apart from the group's other fields; change is
     * what it moves, for Atividade and its push.
     */
    suspend fun updateSimplifiesDebts(
        groupId: String,
        simplifiesDebts: Boolean,
        change: SimplifyDebtsChange,
        changedAt: Long,
    ): Result<Unit>

    // Why: "Ana desligou depois de você" (13.1e) was shown.
    suspend fun consumeSimplifyDebtsConflict(groupId: String): Result<Unit>

    // Why: the incomes this device typed for "Pela renda", empty when it typed none.
    suspend fun getSplitIncomes(groupId: String): Result<Map<String, Long>>

    suspend fun addMember(
        groupId: String,
        member: GroupMember,
        addedAt: Long,
    ): Result<Unit>

    suspend fun updateMemberName(
        memberId: String,
        name: String,
        updatedAt: Long,
    ): Result<Unit>

    // Why: hidden right away, and deleted on the server by the next sync.
    suspend fun removeMember(
        memberId: String,
        removedAt: Long,
    ): Result<Unit>

    /*
     * Why: "Trocar link", online only. The server makes the new code, and the device shows it right away instead of
     * waiting for the next sync.
     */
    suspend fun rotateInviteCode(groupId: String): Result<Unit>

    /*
     * Why: "Liberar nome", online only: the server gives the member a new personal invite code and turns it back into
     * someone nobody joined as, and the device takes the member as the server answers it.
     */
    suspend fun requestMemberRelease(memberId: String): Result<Unit>

    // Why: "Sair do grupo", online only: the server checks this person's balance; then the group leaves the device.
    suspend fun requestLeaveGroup(groupId: String): Result<GroupExitOutcome>

    // Why: "Apagar grupo", online only: the server checks nobody owes anything; then the group leaves the device.
    suspend fun deleteGroup(groupId: String): Result<GroupExitOutcome>

    /*
     * Why: "Apagar grupo" takes the group off every screen at once, while the server is asked, so nobody waits on it;
     * the group stays on the device until the server deleted it. Only while the app runs: a deletion the app closed
     * on is the server's to settle, and the next sync takes the group off or keeps it.
     */
    fun hideGroup(groupId: String)

    /*
     * Why: the server answered: a group it turned down comes back where it was, and one it deleted, already gone from
     * the device, no longer costs the screens a filter.
     */
    fun showGroup(groupId: String)

    suspend fun uploadPendingGroups(userId: String): Result<Unit>

    /*
     * Why: after a switch, another user's groups give way to this user's, even ones not pushed yet, and the synced
     * groups and members the server no longer has leave the device. A group this user left, or a member deleted,
     * leaves it even with changes not pushed yet, and one someone else deleted posts its "Grupo apagado".
     */
    suspend fun downloadGroups(userId: String): Result<Unit>

    // Why: the groups a first download brings (5.1d), the latest activity first, before any of them comes down.
    suspend fun fetchFirstDownloadGroupIds(): Result<List<String>>

    /*
     * Why: one group and its members, as a first download brings each group whole before the next. It only reads the
     * server; the write it returns puts them on the device, so the sync writes a group's rows together, with no round
     * trip in between. Nothing leaves the device, which the full download that follows decides.
     */
    suspend fun fetchGroupDownload(
        userId: String,
        groupId: String,
    ): Result<suspend () -> Unit>

    // Why: a member's photo changed, heard in real time: the faces take the server's photos, as each group download does.
    suspend fun downloadMemberPhotos(): Result<Unit>

    // Why: a sign out leaves no group on the device.
    suspend fun clear()
}
