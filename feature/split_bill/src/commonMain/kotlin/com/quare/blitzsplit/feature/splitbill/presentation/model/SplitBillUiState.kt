package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryBranch
import com.quare.blitzsplit.core.expenses.domain.model.CategoryDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CategoryEditor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatch
import com.quare.blitzsplit.core.expenses.domain.model.CategoryNames
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.ReceiptPhotoSource

data class SplitBillUiState(
    // Why: "Editar conta" rather than "Rachar conta", with "Apagar conta" under the form.
    val isEditing: Boolean,
    // Why: the group the bill goes to, which a new bill may change ("Rachar em qual grupo?").
    val groupId: String,
    // Why: the group "Rachar em qual grupo?" last confirmed, which groupId becomes once it is read.
    val pickedGroupId: String,
    val groupName: String,
    val groupInitials: String,
    val groupColor: AvatarColor,
    // Why: drawn in place of the color and the initials, null for none.
    val groupPhotoUrl: String?,
    // Why: the groups a new bill may go to, the most recently used first; empty while editing.
    val groups: List<BillGroup>,
    // Why: the "Rachar em qual grupo?" sheet, open while not null, with what is marked in it until "Continuar".
    val groupSelection: GroupSelection?,
    // Why: the bill's own currency, the single source of every amount rule below; the group's until one is picked.
    val currency: GroupCurrency,
    // Why: the currency picked beside "Valor" (or the one a bill being edited was saved in); null follows the group's.
    val pickedCurrency: GroupCurrency?,
    // Why: bitcoin typed in whole satoshis while Preferências' "Bitcoin em satoshis" is on.
    val showsSats: Boolean,
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
    /*
     * Why: the category whose exception (6.7f) covers the bill's, even one that no longer adds up and so opens nothing:
     * what "Usar como padrão do grupo" replaces; null for the group's own default.
     */
    val defaultSplitExceptionId: String?,
    // Why: the bill's split becomes the group's default when it is saved, set by Dividir's "Pronto"; null for none.
    val splitAsDefault: SplitAsDefault?,
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
    // Why: Dividir's "Usar como padrão do grupo", which the bill takes on "Pronto" while it shows.
    val isSplitEditorAsDefaultChecked: Boolean,
    // Why: "Itens da conta" (7.5), Dividir's fifth mode, open in its place while not null; the bill changes on "Pronto".
    val itemsEditor: ItemsEditor?,
    // Why: "Item" (7.6), open over "Itens da conta" while not null; the items change on its "Pronto".
    val itemEditor: ItemEditor?,
    // Why: "Apagar este item?" (7.6h), open while not null.
    val itemDeletion: ItemDeletion?,
    // Why: the service's percentage or the couvert per person, open over "Itens da conta" while not null.
    val feeEditor: FeeEditor?,
    // Why: "Ler cupom fiscal" (7.1e to 7.1g), null until a receipt's photo is picked.
    val receipt: BillReceipt?,
    // Why: the receipt's category, which the bill takes while the description's words choose none and none is picked.
    val receiptCategoryId: String?,
    /*
     * Why: "Ler cupom fiscal" asks where the photo comes from first (beta feedback, part 2, item 7), then opens the
     * source picked, until a photo comes or it closes; null for none.
     */
    val receiptRequest: ReceiptRequest?,
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
        get() = AmountInput.parse(amountText, currency, showsSats)

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

    // Why: the list the currency beside "Valor" offers, as "Moeda" of Novo grupo does, the cryptocurrencies last.
    val currencies: List<GroupCurrency>
        get() = GroupCurrency.entries

    /*
     * Why: the currency beside "Valor" is changed until the bill is saved, but not while a receipt is being read, nor
     * while the group picked is read, whose arrival would put the bill back in that group's currency.
     */
    val canChangeCurrency: Boolean
        get() = isLoaded && !isSaving && !isDeleting && !isFormLocked && !isPickedGroupLoading

    // Why: Salvar stays disabled until the bill can be saved.
    val canSave: Boolean
        get() = isLoaded && isAmountValid && isDescriptionValid && isPayerValid && split.addsUp && !isSaving &&
            !isDeleting && !isFormLocked

    /*
     * Why: "Ler cupom fiscal" under "Divisão", only on a new bill and until a receipt is picked (7.1e). Never in bitcoin
     * or ether, which no receipt is printed in, and the reader can't convert to.
     */
    val showsScanReceipt: Boolean
        get() = !isEditing && receipt == null && !currency.isCrypto

    // Why: reading needs the internet; offline it shows disabled with "Precisa de internet" (7.1d).
    val canScanReceipt: Boolean
        get() = !isEditing && isLoaded && isOnline && !isSaving && !isReadingReceipt && !currency.isCrypto

    /*
     * Why: "Tirar foto" or the gallery's choice, once "Rachar em qual grupo?" was answered, so the photo is read in its
     * currency. The groups arrive apart from the bill's group, so it waits for them too: the question may be about to
     * open. Início's "Ler cupom fiscal" on a group in a cryptocurrency opens the bill alone.
     */
    val showsReceiptSourceChoice: Boolean
        get() = isReceiptSourceRequested && canOpenReceiptSource

    // Why: Início's photo, read once the same conditions as the choice hold (beta feedback, part 3, item 16).
    val readsPickedReceipt: Boolean
        get() = receiptRequest == ReceiptRequest.Picked && canOpenReceiptSource

    // Why: the choice asked for, shown or still waiting for its conditions.
    val isReceiptSourceRequested: Boolean
        get() = receiptRequest == ReceiptRequest.Choice

    // Why: the source picked or open without asking, shown or not.
    val receiptSource: ReceiptPhotoSource?
        get() = (receiptRequest as? ReceiptRequest.Open)?.source

    // Why: the camera, the gallery or the file picker picked on the choice, under the same conditions as the choice.
    val shownReceiptSource: ReceiptPhotoSource?
        get() = receiptSource?.takeIf { canOpenReceiptSource }

    private val canOpenReceiptSource: Boolean
        get() = isLoaded && groups.isNotEmpty() && groupSelection == null && !isSaving && !currency.isCrypto &&
            !isPickedGroupLoading

    // Why: until the group picked is read, the currency is still the last group's, which says nothing about its receipt.
    val isPickedGroupLoading: Boolean
        get() = pickedGroupId != groupId

    val isReadingReceipt: Boolean
        get() = receipt?.status == ReceiptStatus.READING

    /*
     * Why: while "Lendo o cupom…" every other field of the form shows disabled (valor, descrição, categoria, grupo, quem
     * pagou, quando, divisão) and any editor open closes, so nothing typed is overwritten by what the read fills; × on
     * "Foto do cupom" still cancels it.
     */
    val isFormLocked: Boolean
        get() = isReadingReceipt

    /*
     * Why: why the receipt read nothing (7.1g), each worded its own way: "Não deu para ler o cupom…" with "Tirar outra
     * foto", the reader that didn't answer and a dropped connection with "Tentar de novo", the day's limit with neither.
     */
    val receiptError: ReceiptError?
        get() = receipt?.error?.takeIf { receipt.status == ReceiptStatus.FAILED }

    // Why: "Tirar outra foto", for a photo nothing could be read from.
    val offersReceiptRetake: Boolean
        get() = receiptError == ReceiptError.UNREADABLE

    // Why: "Tentar de novo" sends the same photo again, when the photo wasn't what failed.
    val offersReceiptRetry: Boolean
        get() = receiptError?.canRetry == true

    /*
     * Why: a new bill's group opens "Rachar em qual grupo?", once there is another group to pick; a saved one stays. Not
     * while a receipt is being read, whose amounts come back in the currency it was sent with.
     */
    val canChangeGroup: Boolean
        get() = !isEditing && isLoaded && groups.size > 1 && !isSaving && !isFormLocked

    // Why: "Padrão do grupo" under Rachar conta's "Divisão" while the bill keeps the group's default split.
    val splitFollowsDefault: Boolean
        get() = defaultSplit?.isFollowedBy(split) == true

    // Why: what Rachar conta's "Divisão" says under its title, "Padrão do grupo" first while it follows the default.
    val splitSummary: List<SplitFact>
        get() = listOfNotNull(
            SplitFact.GroupDefault.takeIf { splitFollowsDefault },
            SplitFact.NewGroupDefault.takeIf { splitAsDefault != null },
        ) + split.summary

    // Why: Dividir's notice above the modes, "Padrão do Apê 302: Partes, 3 para 1", for a group with a default.
    val showsDefaultSplitNotice: Boolean
        get() = defaultSplit != null && splitEditor != null

    // Why: "Diferente do padrão do grupo" with "Voltar ao padrão", once Dividir changed it for this bill.
    val isSplitEditorOffDefault: Boolean
        get() {
            val editor = splitEditor ?: return false
            return defaultSplit?.isFollowedBy(editor) == false
        }

    /*
     * Why: Dividir's "Usar como padrão do grupo" (beta feedback 11), on a new bill split by Igual, Partes or % in another
     * way than the group's default, or its category's exception; without one, anything that doesn't split equally among
     * everyone (Partes at 1 each included), which is what a group without a default does.
     */
    val offersSplitAsDefault: Boolean
        get() {
            val editor = splitEditor ?: return false
            // Why: Itens opens in Dividir's place and leaves its last mode behind.
            if (isEditing || itemsEditor != null || editor.mode.defaultMode == null) return false
            val default = defaultSplit ?: return !editor.splitsEquallyAmongEveryone
            return !default.isFollowedBy(editor)
        }

    // Why: "Usar como padrão do grupo para Mercado", the exception the box replaces, whether or not it opened the bill.
    val splitEditorDefaultException: Category?
        get() = defaultSplitExceptionId?.let { categories.resolve(it) }

    // Why: "Vira a exceção do grupo para Mercado" under "Divisão", for the exception the bill's split replaces.
    val splitAsDefaultException: Category?
        get() = splitAsDefault?.exceptionCategoryId?.let { categories.resolve(it) }

    // Why: the bill's category as it reads: a deleted group category as its parent, none as "Outros".
    val category: Category
        get() = categories.resolve(categoryId)

    // Why: "Alimentação › Mercado": the main category above a subcategory, null for a main one.
    val categoryParent: Category?
        get() = category.parentId?.let { categories.findOrNull(it) }

    /*
     * Why: the category the receipt suggests (7.1e), null without a receipt read or one this tree doesn't know; a group
     * category a sync deleted since reads as where its bills go (7.8f), as the bill's own does.
     */
    val receiptCategory: Category?
        get() = receiptCategoryId?.let { categories.readAsOrNull(it) }?.takeIf { !it.isOther }

    // Why: the category the map's term chooses, null when no term is in the description.
    val mapCategory: Category?
        get() = categoryMatches.firstOrNull()?.let { categories.findOrNull(it.categoryId) }

    /*
     * Why: "Ensinar ao mapa?" (9.3e) once a new bill is saved in a category picked by hand that the map wouldn't pick,
     * Outros never.
     */
    fun teachOfferOrNull(expenseId: String): CategoryTeachOffer? {
        val category = category
        if (isEditing || !isCategoryPicked || category.isOther || category.id == mapCategory?.id) return null
        return CategoryTeachOffer(
            expenseId = expenseId,
            groupId = groupId,
            title = description.trim(),
            categoryId = category.id,
            mapCategoryId = mapCategory?.id,
        )
    }

    val categoryHint: CategoryHint
        get() {
            val mapCategory = mapCategory
            return when {
                isEditing -> CategoryHint.PICKED
                isCategoryPicked && mapCategory != null && mapCategory.id != category.id -> CategoryHint.CHANGED
                isCategoryPicked && mapCategory == null -> CategoryHint.PICKED
                categoryMatches.size > 1 -> CategoryHint.BY_LONGEST_TERM
                categoryMatches.size == 1 -> CategoryHint.BY_TERM
                receiptCategory != null && receiptCategory?.id == category.id -> CategoryHint.BY_RECEIPT
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
            val fromReceipt = receiptCategory?.let { listOf(it to CategorySuggestionReason.RECEIPT) }.orEmpty()
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
            return (fromMap + fromReceipt + used + ofGroup)
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

    /*
     * Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" (7.8) over the picker, as the map opens it too,
     * with "Apagar “X”?" over it.
     */
    val categoryEditor: CategoryEditor?
        get() = categoryForm?.let(::categoryEditorOf)

    // Why: also the form a page still fading out holds, once categoryForm is gone.
    fun categoryEditorOf(form: CategoryForm): CategoryEditor = CategoryEditor(
        form = form,
        categories = categories,
        groupName = groupName,
        creatorName = people.firstOrNull { it.id == form.createdByMemberId }?.name,
        usage = CategoryEditor.usageOf(categories, form.categoryId, categoryUsage),
        deletion = categoryDeletion,
        isOnline = isOnline,
    )

    // Why: "Apagar conta" only when editing, and not while saving or deleting.
    val canDelete: Boolean
        get() = isEditing && isLoaded && !isSaving && !isDeleting

    // Why: "Mudar o total refaz a divisão por valor proporcionalmente.", under the amount next to its other help.
    val showsProportionalAmountHint: Boolean
        get() = isEditing && split.mode == SplitMode.AMOUNT && !showsAmountError

    // Why: "Sem internet: a conta fica salva no aparelho e entra nos saldos agora."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    /*
     * Why: what Salvar stores: none while it reads as "Outros" without being picked, and the id as it is otherwise, so
     * a deleted category keeps reading as its parent and one whose row hasn't come down yet isn't saved as "Outros".
     */
    val categoryIdToSave: String?
        get() = categoryId

    // Why: "Qualquer pessoa do grupo pode editar ou apagar." under the form when editing, unless a warning says more.
    val showsAnyoneCanEditHint: Boolean
        get() = isEditing && conflict == null && settledPair == null

    private companion object {
        const val MAX_SUGGESTIONS = 4
    }
}
