package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryBranch
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatch
import com.quare.blitzsplit.core.expenses.domain.model.CategoryRules
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

data class SplitBillUiState(
    // Why: "Editar conta" rather than "Rachar conta", with "Apagar conta" under the form.
    val isEditing: Boolean,
    // Why: the group the bill goes to, which a new bill may change ("Rachar em qual grupo?").
    val groupId: String,
    val groupName: String,
    val groupInitials: String,
    val groupColor: AvatarColor,
    // Why: the groups a new bill may go to, the most recently used first; empty while editing.
    val groups: List<BillGroup>,
    // Why: the "Rachar em qual grupo?" sheet, open while not null, with what is marked in it until "Continuar".
    val groupSelection: GroupSelection?,
    val currency: GroupCurrency,
    // Why: in the order they were added to the group, the creator first.
    val people: List<BillPerson>,
    // Why: the "Valor" field as typed, "312,00" or "312.00".
    val amountText: String,
    val description: String,
    // Why: a field's error only shows once it was typed in, so a new bill doesn't open in red.
    val hasEditedAmount: Boolean,
    val hasEditedDescription: Boolean,
    // Why: whoever paid the whole bill, empty while several people paid it.
    val payerId: String,
    // Why: "Várias pessoas pagaram", each one with what they paid; null while one person paid the whole bill.
    val severalPayers: PayerAmounts?,
    // Why: the "Quem pagou" sheet, open while not null, with what is marked in it until "Pronto".
    val payerSelection: PayerSelection?,
    // Why: the "Várias pessoas pagaram" sheet, open while not null; it changes who paid only on "Pronto".
    val payerAmounts: PayerAmounts?,
    // Why: "Quando", date and time; each UI writes the date, after "Hoje" or "Ontem" when it is one of them.
    val spentAt: Long,
    val spentDay: RelativeDay,
    // Why: a bill is spent by now, so "Quando" picks nothing later; the time of the last change, which pickers cap at.
    val latestSpentAt: Long,
    val split: SplitEditor,
    // Why: the group's "Divisão padrão" a new bill opens with; null while editing a saved bill or for a group without one.
    val defaultSplit: BillDefaultSplit?,
    // Why: the app's categories with the group's (7.7), which the bill's category resolves in.
    val categories: CategoryTree,
    // Why: the bill's category, an app key or a group category's id; null for "Outros".
    val categoryId: String?,
    // Why: picked by hand (or a saved bill's), so the map's suggestion no longer follows the description.
    val isCategoryPicked: Boolean,
    // Why: the terms of the map in the description, the one that chooses first (7.1, 7.1l).
    val categoryMatches: List<CategoryMatch>,
    // Why: how many of the group's bills each category has, by the category they read as.
    val categoryUsage: Map<String, Int>,
    // Why: "Categoria" (7.7), open while not null.
    val categoryPicker: CategoryPicker?,
    // Why: "Nova categoria" or "Editar categoria" (7.8), open over the picker while not null.
    val categoryForm: CategoryForm?,
    // Why: "Apagar “Churrasco”?" (7.8f), open while not null.
    val categoryDeletion: CategoryDeletion?,
    // Why: "Dividir", open while not null; it changes the bill's split only on "Pronto".
    val splitEditor: SplitEditor?,
    // Why: "Itens da conta" (7.5), Dividir's fifth mode, open in its place while not null; the bill changes on "Pronto".
    val itemsEditor: ItemsEditor?,
    // Why: "Item" (7.6), open over "Itens da conta" while not null; the items change on its "Pronto".
    val itemEditor: ItemEditor?,
    // Why: "Apagar este item?" (7.6h), open while not null.
    val itemDeletion: ItemDeletion?,
    // Why: the service's percentage or the couvert per person, open over "Itens da conta" while not null.
    val feeEditor: FeeEditor?,
    // Why: someone else saved the bill while it was being edited here.
    val conflict: BillConflict?,
    // Why: a payment made after the bill whose pair's balance the change being typed moves (7.4b), said before saving.
    val settledPair: SettledPairChange?,
    // Why: "Apagar esta conta?", open while not null.
    val deletion: BillDeletion?,
    val isOnline: Boolean,
    val isSaving: Boolean,
    val isDeleting: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    val amountMinor: Long?
        get() = AmountInput.parse(amountText, currency)

    val isAmountValid: Boolean
        get() = ExpenseRules.isAmountValid(amountMinor, currency)

    // Why: "Informe quanto foi a conta.", also for an amount over the limit.
    val showsAmountError: Boolean
        get() = hasEditedAmount && !isAmountValid

    val isDescriptionValid: Boolean
        get() = ExpenseRules.isDescriptionValid(description)

    // Why: "Diga o que foi, por exemplo Mercado."
    val showsDescriptionError: Boolean
        get() = hasEditedDescription && !isDescriptionValid

    // Why: whoever paid the whole bill, null while several people paid it or the payer left the group.
    val payer: BillPerson?
        get() = people.firstOrNull { it.id == payerId }

    val payerIsMe: Boolean
        get() = payer?.isMe == true

    // Why: "Ex-membro pagou": a bill being edited keeps a payer who left the group, unless someone else is picked.
    val isPayerFormerMember: Boolean
        get() = isEditing && isLoaded && payer == null && payerId.isNotEmpty()

    // Why: who paid the bill, whom the cents a split can't share evenly go to.
    val payerIds: List<String>
        get() = severalPayers?.payers?.map { it.memberId } ?: listOf(payerId).filter { it.isNotEmpty() }

    // Why: what this device's user paid of the bill, 0 when they paid nothing.
    val myPaidMinor: Long
        get() {
            val me = people.firstOrNull { it.isMe } ?: return 0L
            val several = severalPayers ?: return if (payerIsMe) amountMinor ?: 0L else 0L
            return several.payers.filter { it.memberId == me.id }.sumOf { it.amountMinor }
        }

    // Why: "Sua parte: R$ 118,00", null while the split doesn't add up or leaves this device's user out.
    val myShareMinor: Long?
        get() {
            val me = people.firstOrNull { it.isMe } ?: return null
            return split.shares?.firstOrNull { it.memberId == me.id }?.amountMinor
        }

    /*
     * Why: "Você pagou e recebe R$ 249,60 de volta.", while this device's user paid more than their part. Null while
     * they paid nothing or what they paid doesn't add up with the others yet; with several payers also when nothing
     * comes back, so "Sua parte" shows instead of a refund of R$ 0,00.
     */
    val refundMinor: Long?
        get() {
            if (!isAmountValid || split.shares == null || severalPayers?.addsUp == false) return null
            val paid = myPaidMinor.takeIf { it > 0 } ?: return null
            val refund = paid - (myShareMinor ?: 0L)
            return refund.takeIf { it > 0 || (it == 0L && severalPayers == null) }
        }

    // Why: who paid and how much as Salvar saves it: several people, or one paying the whole amount.
    val payersToSave: List<ExpensePayer>
        get() = severalPayers?.payers ?: listOf(
            ExpensePayer(
                memberId = payerId,
                amountMinor = amountMinor ?: 0L,
            ),
        )

    // Why: who paid is settled: one person (or one who left a bill being edited), or several adding up to the total.
    val isPayerValid: Boolean
        get() = severalPayers?.addsUp ?: (payer != null || isPayerFormerMember)

    // Why: Salvar stays disabled until the bill can be saved.
    val canSave: Boolean
        get() = isLoaded && isAmountValid && isDescriptionValid && isPayerValid && split.addsUp && !isSaving &&
            !isDeleting

    // Why: a new bill's group opens "Rachar em qual grupo?", once there is another group to pick; a saved one stays.
    val canChangeGroup: Boolean
        get() = !isEditing && isLoaded && groups.size > 1 && !isSaving

    // Why: "Padrão do grupo" under Rachar conta's "Divisão" while the bill keeps the group's default split.
    val splitFollowsDefault: Boolean
        get() = defaultSplit?.isFollowedBy(split) == true

    // Why: what Rachar conta's "Divisão" says under its title, "Padrão do grupo" first while it follows the default.
    val splitSummary: List<SplitFact>
        get() = listOfNotNull(SplitFact.GroupDefault.takeIf { splitFollowsDefault }) + split.summary

    // Why: Dividir's notice above the modes, "Padrão do Apê 302: Partes, 3 para 1", for a group with a default.
    val showsDefaultSplitNotice: Boolean
        get() = defaultSplit != null && splitEditor != null

    // Why: "Diferente do padrão do grupo" with "Voltar ao padrão", once Dividir changed it for this bill.
    val isSplitEditorOffDefault: Boolean
        get() {
            val editor = splitEditor ?: return false
            return defaultSplit?.isFollowedBy(editor) == false
        }

    // Why: the bill's category as it reads: a deleted group category as its parent, none as "Outros".
    val category: Category
        get() = categories.resolve(categoryId)

    // Why: "Alimentação › Mercado": the main category above a subcategory, null for a main one.
    val categoryParent: Category?
        get() = category.parentId?.let { categories.findOrNull(it) }

    // Why: the category the map's term chooses, null when no term is in the description.
    val mapCategory: Category?
        get() = categoryMatches.firstOrNull()?.let { categories.findOrNull(it.categoryId) }

    val categoryHint: CategoryHint
        get() {
            val mapCategory = mapCategory
            return when {
                isEditing -> CategoryHint.PICKED
                isCategoryPicked && mapCategory != null && mapCategory.id != category.id -> CategoryHint.CHANGED
                isCategoryPicked && mapCategory == null -> CategoryHint.PICKED
                categoryMatches.size > 1 -> CategoryHint.BY_LONGEST_TERM
                categoryMatches.size == 1 -> CategoryHint.BY_TERM
                else -> CategoryHint.NO_TERM
            }
        }

    // Why: Dividir's notice and the bill's "Divisão" name the exception's category ("Exceção do grupo para Mercado").
    val defaultSplitException: Category?
        get() = defaultSplit?.exceptionCategoryId?.let { categories.resolve(it) }

    /*
     * Why: "Sugeridas" (7.7): the map's choice first ("Pela descrição"), then the group's most used ("Mais usada no
     * grupo" on the first), and the group's own categories ("Do grupo"), four at most.
     */
    val categorySuggestions: List<CategorySuggestion>
        get() {
            val fromMap = mapCategory?.let { listOf(it to CategorySuggestionReason.DESCRIPTION) }.orEmpty()
            val used = categoryUsage.entries
                .filter { (id, _) -> categories.findOrNull(id)?.isOther == false }
                .sortedByDescending { it.value }
                .mapIndexedNotNull { index, (id, _) ->
                    val usedCategory = categories.findOrNull(id) ?: return@mapIndexedNotNull null
                    usedCategory to when {
                        usedCategory.isGroupCategory -> CategorySuggestionReason.GROUP
                        index == 0 -> CategorySuggestionReason.MOST_USED
                        else -> CategorySuggestionReason.USED
                    }
                }
            val ofGroup = categories.all.filter { it.isGroupCategory }.map { it to CategorySuggestionReason.GROUP }
            return (fromMap + used + ofGroup)
                .distinctBy { it.first.id }
                .take(MAX_SUGGESTIONS)
                .map { (suggested, reason) ->
                    CategorySuggestion(
                        category = suggested,
                        reason = reason,
                        isSelected = suggested.id == category.id,
                    )
                }
        }

    // Why: "Restaurante é a mais usada no Viagem Floripa."
    val mostUsedCategory: Category?
        get() = categorySuggestions.firstOrNull { it.reason == CategorySuggestionReason.MOST_USED }?.category

    // Why: what "Buscar categoria" finds, by any word of the name starting with what is typed, in the list's order.
    val categoryResults: List<Category>
        get() {
            val query = categoryPicker?.query?.trim().orEmpty()
            if (query.isEmpty()) return emptyList()
            return categories.all.filter { CategoryNames.matches(it, query) }
        }

    val isSearchingCategories: Boolean
        get() = categoryPicker?.query?.isNotBlank() == true

    // Why: "Nenhuma categoria com “Pet”" with "Criar “Pet”" (7.7c).
    val showsNoCategoryFound: Boolean
        get() = isSearchingCategories && categoryResults.isEmpty()

    // Why: the main category whose subcategories show in the picker, the bill's own to start.
    val openCategoryBranch: CategoryBranch?
        get() = categoryPicker?.openMainId?.let { categories.branchOf(it) }

    // Why: "Já existe Churrasco em Alimentação. Escolha outro nome." (7.8c), among the app's names and the group's.
    val isCategoryNameTaken: Boolean
        get() {
            val form = categoryForm ?: return false
            val name = form.name.trim()
            if (name.isEmpty()) return false
            return categories.hasGroupName(form.parentId, name, form.categoryId) ||
                categories.all.any { it.app != null && it.parentId == form.parentId && CategoryNames.isNamed(it, name) }
        }

    // Why: a category that has subcategories stays a main one, as there are only two levels.
    val isCategoryParentLocked: Boolean
        get() = categoryForm?.categoryId?.let(categories::hasSubcategories) == true

    // Why: "Dentro de": "Nenhuma: categoria principal" and every main category but the one being edited.
    val categoryParents: List<Category>
        get() = categories.branches.map { it.category }.filter { it.id != categoryForm?.categoryId && !it.isOther }

    // Why: "Criada por Ana · usada em 3 contas", the group's bills with it or a subcategory of it.
    val categoryFormUsage: Int
        get() {
            val id = categoryForm?.categoryId ?: return 0
            val subcategories = categories.branchOf(id)?.subcategories.orEmpty().map { it.id }
            return (listOf(id) + subcategories).sumOf { categoryUsage[it] ?: 0 }
        }

    val canSaveCategory: Boolean
        get() {
            val form = categoryForm ?: return false
            return CategoryRules.isNameValid(form.name) && !isCategoryNameTaken && !form.isSaving
        }

    // Why: "Apagar conta" only when editing, and not while saving or deleting.
    val canDelete: Boolean
        get() = isEditing && isLoaded && !isSaving && !isDeleting

    // Why: "Mudar o total refaz a divisão por valor proporcionalmente.", under the amount next to its other help.
    val showsProportionalAmountHint: Boolean
        get() = isEditing && split.mode == SplitMode.AMOUNT && !showsAmountError

    // Why: "Sem internet: a conta fica salva no aparelho e entra nos saldos agora."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: what Salvar stores: none while it reads as "Outros" without being picked.
    val categoryIdToSave: String?
        get() = category.id.takeUnless { categoryId == null && it == AppCategory.OTHER.key }

    // Why: "Qualquer pessoa do grupo pode editar ou apagar." under the form when editing, unless a warning says more.
    val showsAnyoneCanEditHint: Boolean
        get() = isEditing && conflict == null && settledPair == null

    private companion object {
        const val MAX_SUGGESTIONS = 4
    }
}
