package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface SplitBillUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: typed letter by letter; what was typed shows in the bill saved.
            is OnAmountChanged,
            is OnDescriptionChanged,
            is OnPayerAmountChanged,
            is OnPersonValueChanged,
            is OnItemNameChanged,
            is OnItemQuantityChanged,
            is OnItemPriceChanged,
            is OnItemPersonValueChanged,
            is OnFeeValueChanged,
            is OnCategoryQueryChanged,
            is OnCategoryNameChanged,
            -> EventAnalytics.NotTracked

            /*
             * Why: Apple's and the web's pickers send each step of "Quando" (a day, the time wheel) as it turns, like
             * typing; the tap that opened it counts.
             */
            is OnSpentAtChanged -> EventAnalytics.NotTracked

            // Why: a close or a back, which each platform also does its own way: where it went is its screen view.
            OnGroupDismissed,
            OnPayerDismissed,
            OnSeveralPayersDismissed,
            OnSplitDismissed,
            OnItemsDismissed,
            OnItemDismissed,
            OnFeeDismissed,
            OnCategoryDismissed,
            OnCategoryFormDismissed,
            OnReceiptCameraDismissed,
            OnCloseClicked,
            -> EventAnalytics.NotTracked

            OnGroupClicked -> EventAnalytics.Automatic("split_bill_group_clicked")

            // Why: only a pick the sheet takes, never a late tap once it closed.
            is OnGroupPicked -> EventAnalytics.Manual("split_bill_group_picked")

            // Why: only "Continuar" on a question still open, with whether it moved the bill to another group.
            OnGroupDone -> EventAnalytics.Manual("split_bill_group_confirmed")

            OnPayerClicked -> EventAnalytics.Automatic("split_bill_payer_clicked")

            // Why: only a pick the sheet takes, never a late tap once it closed.
            is OnPayerPicked -> EventAnalytics.Manual("split_bill_payer_picked")

            // Why: only "Pronto" on a sheet still open.
            OnPayerDone -> EventAnalytics.Manual("split_bill_payer_confirmed")

            OnSeveralPayersClicked -> EventAnalytics.Automatic("split_bill_several_payers_clicked")

            // Why: only once the amounts add up, with how many paid.
            OnSeveralPayersDone -> EventAnalytics.Manual("split_bill_several_payers_confirmed")

            OnSpentAtOpened -> EventAnalytics.Automatic("split_bill_date_clicked")

            OnSplitClicked -> EventAnalytics.Automatic("split_bill_split_clicked")

            // Why: only while the receipt's items are there to open, in the bill's currency.
            OnReceiptItemsClicked -> EventAnalytics.Manual("receipt_items_clicked")

            // Why: only another mode, never the one picked, as Apple's Picker reports only a change.
            is OnSplitModeClicked -> EventAnalytics.Manual("split_bill_split_mode_selected")

            // Why: toggles, sent with where they ended up.
            is OnPersonToggled -> EventAnalytics.Manual("split_bill_person_toggled")

            OnEveryoneToggled -> EventAnalytics.Manual("split_bill_everyone_toggled")

            OnSplitAsDefaultToggled -> EventAnalytics.Manual("split_bill_split_as_default_toggled")

            is OnPartsIncreased -> EventAnalytics.Automatic("split_bill_parts_changed", mapOf("direction" to "up"))

            is OnPartsDecreased -> EventAnalytics.Automatic("split_bill_parts_changed", mapOf("direction" to "down"))

            OnSplitResetToDefault -> EventAnalytics.Automatic("split_bill_split_reset_clicked")

            // Why: only once the split adds up and Dividir closes, with the mode picked.
            OnSplitDone -> EventAnalytics.Manual("split_bill_split_confirmed")

            // Why: only once every item has someone and they close with the total.
            OnItemsDone -> EventAnalytics.Manual("bill_items_confirmed")

            OnAddItemClicked -> EventAnalytics.Automatic("bill_item_add_clicked")

            OnAddDifferenceItemClicked -> EventAnalytics.Automatic("bill_items_difference_item_clicked")

            // Why: only while there is a difference to share.
            OnSplitDifferenceClicked -> EventAnalytics.Manual("bill_items_difference_split_clicked")

            OnDifferenceRemoved -> EventAnalytics.Automatic("bill_items_difference_removed")

            is OnItemClicked -> EventAnalytics.Automatic("bill_item_clicked")

            OnItemQuantityIncreased -> EventAnalytics.Automatic(
                "bill_item_quantity_changed",
                mapOf("direction" to "up"),
            )

            OnItemQuantityDecreased -> EventAnalytics.Automatic(
                "bill_item_quantity_changed",
                mapOf("direction" to "down"),
            )

            // Why: only another way to split the item, never the one picked.
            is OnItemModeClicked -> EventAnalytics.Manual("bill_item_mode_selected")

            // Why: a toggle, sent with where it ended up.
            is OnItemPersonToggled -> EventAnalytics.Manual("bill_item_person_toggled")

            is OnItemPersonIncreased -> EventAnalytics.Automatic("bill_item_person_changed", mapOf("direction" to "up"))

            is OnItemPersonDecreased -> EventAnalytics.Automatic(
                "bill_item_person_changed",
                mapOf("direction" to "down"),
            )

            // Why: only once the item has a name, a price and someone, with its mode and whether it is new.
            OnItemDone -> EventAnalytics.Manual("bill_item_confirmed")

            // Why: only when its question opens, an item still in the list.
            is OnItemDeleteClicked -> EventAnalytics.Manual("bill_item_delete_clicked")

            // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
            OnItemDeleteConfirmed -> EventAnalytics.Manual("bill_item_delete_confirmed")

            OnItemDeleteDismissed -> EventAnalytics.Manual("bill_item_delete_cancelled")

            // Why: only when its field opens, over "Itens da conta".
            is OnFeeClicked -> EventAnalytics.Manual("bill_items_fee_clicked")

            // Why: only a value the field takes.
            OnFeeDone -> EventAnalytics.Manual("bill_items_fee_confirmed")

            // Why: only once the categories are read, since Categoria opens only then.
            OnCategoryClicked -> EventAnalytics.Manual("bill_category_clicked")

            // Why: a toggle, sent with where it ended up; Outros is picked instead, so it counts as a pick.
            is OnCategoryMainToggled -> EventAnalytics.Manual(
                setOf("bill_category_main_toggled", "bill_category_picked"),
            )

            // Why: only a category still there, never one a sync just deleted.
            is OnCategoryPicked -> EventAnalytics.Manual("bill_category_picked")

            OnCategoryMapClicked -> EventAnalytics.Automatic(
                "category_map_clicked",
                mapOf("source" to SPLIT_BILL_SOURCE),
            )

            is OnNewCategoryClicked -> EventAnalytics.Automatic(
                "category_create_clicked",
                mapOf("is_subcategory" to (parentId != null), "source" to SPLIT_BILL_SOURCE),
            )

            // Why: only a category its form opens for, as the map counts it.
            is OnEditCategoryClicked -> EventAnalytics.Manual("category_edit_clicked")

            is OnCategoryParentPicked -> EventAnalytics.Automatic(
                "category_parent_picked",
                mapOf("source" to SPLIT_BILL_SOURCE),
            )

            is OnCategoryIconPicked -> EventAnalytics.Automatic(
                "category_icon_picked",
                mapOf("icon" to icon, "source" to SPLIT_BILL_SOURCE),
            )

            // Why: the tap, then the category once it is saved.
            OnCategoryFormSaveClicked -> EventAnalytics.Manual(setOf("category_save_clicked", "category_saved"))

            // Why: only when its question opens, an answer only while it is open and not deleting yet.
            OnCategoryDeleteClicked -> EventAnalytics.Manual("category_delete_clicked")

            OnCategoryDeleteConfirmed -> EventAnalytics.Manual("category_delete_confirmed")

            OnCategoryDeleteDismissed -> EventAnalytics.Manual("category_delete_cancelled")

            // Why: only while it can read one (online, not editing), as the camera opens only then.
            OnScanReceiptClicked -> EventAnalytics.Manual("scan_receipt_clicked")

            // Why: a photo handed back, which stands for the tap that opened the camera, then how the read ended.
            is OnReceiptPhotoPicked -> EventAnalytics.Manual(setOf("photo_picked", "receipt_read"))

            OnReceiptRetryClicked -> EventAnalytics.Manual(setOf("retry_clicked", "receipt_read"))

            OnReceiptRemoved -> EventAnalytics.Automatic("receipt_removed")

            // Why: the tap, then the bill once it is saved.
            OnSaveClicked -> EventAnalytics.Manual(setOf("split_bill_save_clicked", "bill_saved"))

            // Why: only when "Apagar esta conta?" opens, an answer only while it is open and not deleting yet.
            OnDeleteClicked -> EventAnalytics.Manual("bill_delete_clicked")

            OnDeleteConfirmed -> EventAnalytics.Manual("bill_delete_confirmed")

            OnDeleteDismissed -> EventAnalytics.Manual("bill_delete_cancelled")
        }

    data class OnAmountChanged(
        val text: String,
    ) : SplitBillUiEvent

    data class OnDescriptionChanged(
        val text: String,
    ) : SplitBillUiEvent

    // Why: the group of a new bill, which opens "Rachar em qual grupo?" with it marked.
    data object OnGroupClicked : SplitBillUiEvent

    data class OnGroupPicked(
        val groupId: String,
    ) : SplitBillUiEvent

    // Why: "Continuar" on "Rachar em qual grupo?".
    data object OnGroupDone : SplitBillUiEvent

    // Why: the sheet closed without "Continuar", which leaves the bill in its group.
    data object OnGroupDismissed : SplitBillUiEvent

    // Why: what "Quem pagou" and "Várias pessoas pagaram" send, which PayerPicking handles.
    sealed interface Payers : SplitBillUiEvent

    // Why: "Quem pagou", which opens the sheet with whoever paid marked, or "Várias pessoas".
    data object OnPayerClicked : Payers

    data class OnPayerPicked(
        val memberId: String,
    ) : Payers

    // Why: "Pronto" on the sheet.
    data object OnPayerDone : Payers

    data object OnPayerDismissed : Payers

    // Why: "Várias pessoas" on "Quem pagou", which opens "Várias pessoas pagaram" in its place.
    data object OnSeveralPayersClicked : Payers

    // Why: how much one person paid, typed on "Várias pessoas pagaram".
    data class OnPayerAmountChanged(
        val memberId: String,
        val text: String,
    ) : Payers

    // Why: "Pronto" on "Várias pessoas pagaram".
    data object OnSeveralPayersDone : Payers

    // Why: "Várias pessoas pagaram" closed without "Pronto", which leaves who paid as it was.
    data object OnSeveralPayersDismissed : Payers

    // Why: "Quando" opening its picker, which then stops at the day it is now, also once midnight passed meanwhile.
    data object OnSpentAtOpened : SplitBillUiEvent

    // Why: "Quando", picked in each platform's date and time picker.
    data class OnSpentAtChanged(
        val epochMillis: Long,
    ) : SplitBillUiEvent

    // Why: "Divisão", which opens "Dividir".
    data object OnSplitClicked : SplitBillUiEvent

    // Why: "14 itens" beside "Lido do cupom", which opens Dividir straight on "Itens", the receipt's items in it.
    data object OnReceiptItemsClicked : SplitBillUiEvent

    // Why: Igual, Valores, % or Partes; Itens opens "Itens da conta" in Dividir's place.
    data class OnSplitModeClicked(
        val mode: SplitMode,
    ) : SplitBillUiEvent

    // Why: the checkbox of "Igual".
    data class OnPersonToggled(
        val memberId: String,
    ) : SplitBillUiEvent

    // Why: "Marcar todos" or "Desmarcar todos".
    data object OnEveryoneToggled : SplitBillUiEvent

    // Why: the field of "Valores" or "%".
    data class OnPersonValueChanged(
        val memberId: String,
        val text: String,
    ) : SplitBillUiEvent

    // Why: the stepper of "Partes".
    data class OnPartsIncreased(
        val memberId: String,
    ) : SplitBillUiEvent

    data class OnPartsDecreased(
        val memberId: String,
    ) : SplitBillUiEvent

    // Why: "Voltar ao padrão" on Dividir's notice, back to the group's "Divisão padrão" for this bill.
    data object OnSplitResetToDefault : SplitBillUiEvent

    // Why: Dividir's "Usar como padrão do grupo" (beta feedback 11), checked or not.
    data object OnSplitAsDefaultToggled : SplitBillUiEvent

    // Why: "Pronto" on "Dividir".
    data object OnSplitDone : SplitBillUiEvent

    // Why: "Voltar" or "Cancelar" on "Dividir", which leaves the bill's split as it was.
    data object OnSplitDismissed : SplitBillUiEvent

    // Why: what "Itens da conta" (7.5), "Item" (7.6) and their questions send, which ItemsEditing handles.
    sealed interface Items : SplitBillUiEvent

    // Why: back on "Itens da conta", to Dividir, which keeps the items typed so far for Itens to open with again.
    data object OnItemsDismissed : Items

    // Why: "Pronto" on "Itens da conta", which splits the bill by its items and closes Dividir too.
    data object OnItemsDone : Items

    // Why: "Adicionar item", which opens "Item" for a new one.
    data object OnAddItemClicked : Items

    // Why: "Adicionar item de R$ 15,00" (7.5d), a new item priced at what the items fall short of.
    data object OnAddDifferenceItemClicked : Items

    // Why: "Dividir a diferença" (7.5d), shared in proportion to what each one consumed.
    data object OnSplitDifferenceClicked : Items

    // Why: the difference shared that way taken off again.
    data object OnDifferenceRemoved : Items

    // Why: an item of the list, which opens "Item" with it.
    data class OnItemClicked(
        val itemId: String,
    ) : Items

    data class OnItemNameChanged(
        val text: String,
    ) : Items

    // Why: the stepper of "Quantidade".
    data object OnItemQuantityIncreased : Items

    data object OnItemQuantityDecreased : Items

    // Why: "Quantidade" typed, on the web and the Mac.
    data class OnItemQuantityChanged(
        val text: String,
    ) : Items

    // Why: "Preço de cada".
    data class OnItemPriceChanged(
        val text: String,
    ) : Items

    // Why: Igual, Unidades, Partes, Valor or %.
    data class OnItemModeClicked(
        val mode: ItemSplitMode,
    ) : Items

    // Why: the checkbox of Igual.
    data class OnItemPersonToggled(
        val memberId: String,
    ) : Items

    // Why: the stepper of Unidades and Partes.
    data class OnItemPersonIncreased(
        val memberId: String,
    ) : Items

    data class OnItemPersonDecreased(
        val memberId: String,
    ) : Items

    // Why: the field of Valor and %, and the web's and the Mac's of Unidades and Partes.
    data class OnItemPersonValueChanged(
        val memberId: String,
        val text: String,
    ) : Items

    // Why: "Pronto" on "Item".
    data object OnItemDone : Items

    // Why: "Voltar" on "Item", which leaves the items as they were.
    data object OnItemDismissed : Items

    // Why: "Apagar item", from "Item" or the Mac's "−" under the list, which asks first.
    data class OnItemDeleteClicked(
        val itemId: String,
    ) : Items

    data object OnItemDeleteConfirmed : Items

    data object OnItemDeleteDismissed : Items

    // Why: the row of "Serviço" or "Couvert", which opens its field.
    data class OnFeeClicked(
        val fee: ItemsFee,
    ) : Items

    data class OnFeeValueChanged(
        val text: String,
    ) : Items

    data object OnFeeDone : Items

    data object OnFeeDismissed : Items

    // Why: what "Categoria" (7.7), "Nova categoria" (7.8) and their questions send, which CategoryEditing handles.
    sealed interface Categories : SplitBillUiEvent

    // Why: the icon beside the description, which opens "Categoria" (7.7).
    data object OnCategoryClicked : Categories

    data class OnCategoryQueryChanged(
        val text: String,
    ) : Categories

    // Why: a main category of "Todas as categorias", which shows its subcategories, or hides them again.
    data class OnCategoryMainToggled(
        val categoryId: String,
    ) : Categories

    // Why: a suggestion, a subcategory, "Só Alimentação", a result or a category without subcategories: the bill's now.
    data class OnCategoryPicked(
        val categoryId: String,
    ) : Categories

    data object OnCategoryDismissed : Categories

    // Why: the "Mapa de categorias" card between Sugeridas and Todas as categorias (9.0).
    data object OnCategoryMapClicked : SplitBillUiEvent

    /*
     * Why: "Nova" (a main category), "Nova em Alimentação" (its subcategory) or "Criar “Pet”" from a search with the
     * text typed (7.7c, 7.8, 7.8b).
     */
    data class OnNewCategoryClicked(
        val parentId: String?,
        val name: String,
    ) : Categories

    // Why: a long press (⋯ on the web, the context menu on the Mac) on one of the group's categories (7.8e).
    data class OnEditCategoryClicked(
        val categoryId: String,
    ) : Categories

    data class OnCategoryNameChanged(
        val text: String,
    ) : Categories

    // Why: "Dentro de", null for "Nenhuma: categoria principal".
    data class OnCategoryParentPicked(
        val parentId: String?,
    ) : Categories

    data class OnCategoryIconPicked(
        val icon: CategoryIcon,
    ) : Categories

    data object OnCategoryFormSaveClicked : Categories

    // Why: back to "Categoria" without saving.
    data object OnCategoryFormDismissed : Categories

    // Why: "Apagar categoria", which asks first (7.8f).
    data object OnCategoryDeleteClicked : Categories

    data object OnCategoryDeleteConfirmed : Categories

    data object OnCategoryDeleteDismissed : Categories

    // Why: what "Ler cupom fiscal" (7.1e to 7.1g) sends, which ReceiptScanning handles.
    sealed interface Receipt : SplitBillUiEvent

    // Why: "Ler cupom fiscal", and "Tirar outra foto" once it couldn't be read (7.1g): the camera or a file.
    data object OnScanReceiptClicked : Receipt

    // Why: the receipt's photo, from the camera, the gallery or a file, as it came.
    class OnReceiptPhotoPicked(
        val photo: ByteArray,
    ) : Receipt

    // Why: "Tentar de novo" once the reader didn't answer or the connection dropped: the same photo, read again.
    data object OnReceiptRetryClicked : Receipt

    // Why: the camera or the file picker closed without a photo.
    data object OnReceiptCameraDismissed : Receipt

    // Why: "Tirar a foto do cupom": the bill keeps what was filled in, without the photo nor the receipt's items.
    data object OnReceiptRemoved : Receipt

    data object OnSaveClicked : SplitBillUiEvent

    // Why: "Apagar conta" under the form of "Editar conta", which asks first.
    data object OnDeleteClicked : SplitBillUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : SplitBillUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : SplitBillUiEvent

    data object OnCloseClicked : SplitBillUiEvent
}
