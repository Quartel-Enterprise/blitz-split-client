package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ExportGroup
import com.quare.blitzsplit.core.expenses.export.CsvGroupExport
import com.quare.blitzsplit.core.expenses.export.GroupExport
import com.quare.blitzsplit.core.expenses.export.GroupExportFile
import com.quare.blitzsplit.core.expenses.export.GroupExportFormat
import com.quare.blitzsplit.core.expenses.export.GroupExportWords
import com.quare.blitzsplit.core.expenses.export.JsonGroupExport
import com.quare.blitzsplit.core.expenses.export.TextGroupExport
import com.quare.blitzsplit.core.expenses.export.XlsxGroupExport
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/*
 * Why: reads the group once from Room, so it works offline and exports what the device has, then writes the format
 * picked. The file is named after the group and the day, "Trip-to-Rio-2026-11-14.xlsx".
 */
class ExportGroupUseCase(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val categoryRepository: CategoryRepository,
    private val currentTimeZone: CurrentTimeZone,
    private val currentTimeMillis: CurrentTimeMillis,
    // Why: where the file is written, off the main thread; Dispatchers.Default in the app, the test's in tests.
    private val encodeDispatcher: CoroutineDispatcher,
) : ExportGroup {
    /*
     * Why: what a file system refuses in a name on Android, Apple, Windows (a download) or Linux; the control
     * characters as a range, since JavaScript knows no \p{Cntrl}.
     */
    private val forbiddenInName = Regex("""[\\/:*?"<>|\x00-\x1F\x7F]""")
    private val spaces = Regex("""\s+""")

    override suspend fun invoke(
        groupId: String,
        format: GroupExportFormat,
        words: GroupExportWords,
    ): Result<GroupExportFile> = suspendRunCatching {
        val group = groupRepository.observeGroups().first().firstOrNull { it.id == groupId }
            ?: throw NoSuchElementException("The group is no longer on this device")
        val expenses = expenseRepository.observeExpenses(groupId).first()
        val settlements = settlementRepository.observeSettlements(groupId).first()
        val categoryTree = CategoryTree.of(
            categoryRepository.observeGroupCategories(groupId).first(),
            categoryRepository.observeAppColors().first(),
        )
        withContext(encodeDispatcher) {
            val export = GroupExport.of(
                group = group,
                expenses = expenses,
                settlements = settlements,
                categoryTree = categoryTree,
                timeZone = currentTimeZone(),
                nowMillis = currentTimeMillis(),
            )
            val encoder = when (format) {
                GroupExportFormat.CSV -> CsvGroupExport()
                GroupExportFormat.XLSX -> XlsxGroupExport()
                GroupExportFormat.JSON -> JsonGroupExport()
                GroupExportFormat.TXT -> TextGroupExport()
            }
            GroupExportFile(
                fileName = fileNameOf(export.groupName, export.exportedOn.toString(), format),
                format = format,
                bytes = encoder.encode(export, words),
            )
        }
    }

    private fun fileNameOf(
        groupName: String,
        day: String,
        format: GroupExportFormat,
    ): String {
        val name = groupName
            .replace(forbiddenInName, "")
            .trim()
            .trim('.')
            .replace(spaces, "-")
            .take(MAX_NAME_LENGTH)
            .trimEnd('-')
        return listOf(name, day).filter { it.isNotEmpty() }.joinToString("-") + "." + format.extension
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
    }
}
