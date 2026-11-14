package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen Rachar conta's events come from, as their source param says, for its UiEvent and its ViewModel alike.
internal const val SPLIT_BILL_SOURCE = "split_bill"

// Why: what each of Rachar conta's events tracks (`SplitBillUiEvent.analytics`), apart from the events themselves.
internal fun splitBillAnalyticsOf(event: SplitBillUiEvent): EventAnalytics = when (event) {
    // Why: typed letter by letter; what was typed shows in the bill saved.
    is SplitBillUiEvent.OnAmountChanged,
    is SplitBillUiEvent.OnDescriptionChanged,
    is SplitBillUiEvent.OnPayerAmountChanged,
    is SplitBillUiEvent.OnPersonValueChanged,
    is SplitBillUiEvent.OnItemNameChanged,
    is SplitBillUiEvent.OnItemQuantityChanged,
    is SplitBillUiEvent.OnItemPriceChanged,
    is SplitBillUiEvent.OnItemPersonValueChanged,
    is SplitBillUiEvent.OnFeeValueChanged,
    is SplitBillUiEvent.OnCategoryQueryChanged,
    is SplitBillUiEvent.OnCategoryNameChanged,
    -> EventAnalytics.NotTracked

        /*
         * Why: Apple's and the web's pickers send each step of "Quando" (a day, the time wheel) as it turns, like
         * typing; the tap that opened it counts.
         */
    is SplitBillUiEvent.OnSpentDayPicked,
    is SplitBillUiEvent.OnSpentTimePicked,
    -> EventAnalytics.NotTracked

    // Why: a close or a back, which each platform also does its own way: where it went is its screen view.
    SplitBillUiEvent.OnGroupDismissed,
    SplitBillUiEvent.OnPayerDismissed,
    SplitBillUiEvent.OnSeveralPayersDismissed,
    SplitBillUiEvent.OnSplitDismissed,
    SplitBillUiEvent.OnItemsDismissed,
    SplitBillUiEvent.OnItemDismissed,
    SplitBillUiEvent.OnFeeDismissed,
    SplitBillUiEvent.OnCategoryDismissed,
    SplitBillUiEvent.OnCategoryFormDismissed,
    SplitBillUiEvent.OnReceiptPickerDismissed,
    SplitBillUiEvent.OnCloseClicked,
    -> EventAnalytics.NotTracked

    SplitBillUiEvent.OnGroupClicked -> EventAnalytics.Automatic("split_bill_group_clicked")

    // Why: only a tap that opens the group, never one while the bill saves or a receipt is read.
    SplitBillUiEvent.OnOpenGroupClicked -> EventAnalytics.Manual("split_bill_group_opened")

    // Why: only a pick that changes the bill's currency, never the one already there.
    is SplitBillUiEvent.OnCurrencyPicked -> EventAnalytics.Manual("split_bill_currency_picked")

    // Why: only a pick the sheet takes, never a late tap once it closed.
    is SplitBillUiEvent.OnGroupPicked -> EventAnalytics.Manual("split_bill_group_picked")

    // Why: only "Continuar" on a question still open, with whether it moved the bill to another group.
    SplitBillUiEvent.OnGroupDone -> EventAnalytics.Manual("split_bill_group_confirmed")

    SplitBillUiEvent.OnPayerClicked -> EventAnalytics.Automatic("split_bill_payer_clicked")

    // Why: only a pick the sheet takes, never a late tap once it closed.
    is SplitBillUiEvent.OnPayerPicked -> EventAnalytics.Manual("split_bill_payer_picked")

    // Why: only "Pronto" on a sheet still open.
    SplitBillUiEvent.OnPayerDone -> EventAnalytics.Manual("split_bill_payer_confirmed")

    SplitBillUiEvent.OnSeveralPayersClicked -> EventAnalytics.Automatic("split_bill_several_payers_clicked")

    // Why: only once the amounts add up, with how many paid.
    SplitBillUiEvent.OnSeveralPayersDone -> EventAnalytics.Manual("split_bill_several_payers_confirmed")

    SplitBillUiEvent.OnSpentAtOpened -> EventAnalytics.Automatic("split_bill_date_clicked")

    SplitBillUiEvent.OnSpentTimeOpened -> EventAnalytics.Automatic("split_bill_time_clicked")

    SplitBillUiEvent.OnSplitClicked -> EventAnalytics.Automatic("split_bill_split_clicked")

    // Why: only while the receipt's items are there to open, in the bill's currency.
    SplitBillUiEvent.OnReceiptItemsClicked -> EventAnalytics.Manual("receipt_items_clicked")

    // Why: only another mode, never the one picked, as Apple's Picker reports only a change.
    is SplitBillUiEvent.OnSplitModeClicked -> EventAnalytics.Manual("split_bill_split_mode_selected")

    // Why: toggles, sent with where they ended up.
    is SplitBillUiEvent.OnPersonToggled -> EventAnalytics.Manual("split_bill_person_toggled")

    SplitBillUiEvent.OnEveryoneToggled -> EventAnalytics.Manual("split_bill_everyone_toggled")

    SplitBillUiEvent.OnSplitAsDefaultToggled -> EventAnalytics.Manual("split_bill_split_as_default_toggled")

    is SplitBillUiEvent.OnPartsIncreased -> EventAnalytics.Automatic(
        "split_bill_parts_changed",
        mapOf("direction" to "up"),
    )

    is SplitBillUiEvent.OnPartsDecreased -> EventAnalytics.Automatic(
        "split_bill_parts_changed",
        mapOf("direction" to "down"),
    )

    SplitBillUiEvent.OnSplitResetToDefault -> EventAnalytics.Automatic("split_bill_split_reset_clicked")

    // Why: only once the split adds up and Dividir closes, with the mode picked.
    SplitBillUiEvent.OnSplitDone -> EventAnalytics.Manual("split_bill_split_confirmed")

    // Why: only once every item has someone and they close with the total.
    SplitBillUiEvent.OnItemsDone -> EventAnalytics.Manual("bill_items_confirmed")

    SplitBillUiEvent.OnAddItemClicked -> EventAnalytics.Automatic("bill_item_add_clicked")

    SplitBillUiEvent.OnAddDifferenceItemClicked -> EventAnalytics.Automatic("bill_items_difference_item_clicked")

    // Why: only while there is a difference to share.
    SplitBillUiEvent.OnSplitDifferenceClicked -> EventAnalytics.Manual("bill_items_difference_split_clicked")

    SplitBillUiEvent.OnDifferenceRemoved -> EventAnalytics.Automatic("bill_items_difference_removed")

    is SplitBillUiEvent.OnItemClicked -> EventAnalytics.Automatic("bill_item_clicked")

    SplitBillUiEvent.OnItemQuantityIncreased -> EventAnalytics.Automatic(
        "bill_item_quantity_changed",
        mapOf("direction" to "up"),
    )

    SplitBillUiEvent.OnItemQuantityDecreased -> EventAnalytics.Automatic(
        "bill_item_quantity_changed",
        mapOf("direction" to "down"),
    )

    // Why: only another way to split the item, never the one picked.
    is SplitBillUiEvent.OnItemModeClicked -> EventAnalytics.Manual("bill_item_mode_selected")

    // Why: a toggle, sent with where it ended up.
    is SplitBillUiEvent.OnItemPersonToggled -> EventAnalytics.Manual("bill_item_person_toggled")

    is SplitBillUiEvent.OnItemPersonIncreased -> EventAnalytics.Automatic(
        "bill_item_person_changed",
        mapOf("direction" to "up"),
    )

    is SplitBillUiEvent.OnItemPersonDecreased -> EventAnalytics.Automatic(
        "bill_item_person_changed",
        mapOf("direction" to "down"),
    )

    // Why: only once the item has a name, a price and someone, with its mode and whether it is new.
    SplitBillUiEvent.OnItemDone -> EventAnalytics.Manual("bill_item_confirmed")

    // Why: only when its question opens, an item still in the list.
    is SplitBillUiEvent.OnItemDeleteClicked -> EventAnalytics.Manual("bill_item_delete_clicked")

    // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
    SplitBillUiEvent.OnItemDeleteConfirmed -> EventAnalytics.Manual("bill_item_delete_confirmed")

    SplitBillUiEvent.OnItemDeleteDismissed -> EventAnalytics.Manual("bill_item_delete_cancelled")

    // Why: only when its field opens, over "Itens da conta".
    is SplitBillUiEvent.OnFeeClicked -> EventAnalytics.Manual("bill_items_fee_clicked")

    // Why: only a value the field takes.
    SplitBillUiEvent.OnFeeDone -> EventAnalytics.Manual("bill_items_fee_confirmed")

    // Why: only once the categories are read, since Categoria opens only then.
    SplitBillUiEvent.OnCategoryClicked -> EventAnalytics.Manual("bill_category_clicked")

    // Why: a toggle, sent with where it ended up; Outros is picked instead, so it counts as a pick.
    is SplitBillUiEvent.OnCategoryMainToggled -> EventAnalytics.Manual(
        setOf("bill_category_main_toggled", "bill_category_picked"),
    )

    // Why: only a category still there, never one a sync just deleted.
    is SplitBillUiEvent.OnCategoryPicked -> EventAnalytics.Manual("bill_category_picked")

    SplitBillUiEvent.OnCategoryMapClicked -> EventAnalytics.Automatic(
        "category_map_clicked",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    is SplitBillUiEvent.OnNewCategoryClicked -> EventAnalytics.Automatic(
        "category_create_clicked",
        mapOf("is_subcategory" to (event.parentId != null), "source" to SPLIT_BILL_SOURCE),
    )

    // Why: only a category its form opens for, as the map counts it.
    is SplitBillUiEvent.OnEditCategoryClicked -> EventAnalytics.Manual("category_edit_clicked")

    is SplitBillUiEvent.OnCategoryParentPicked -> EventAnalytics.Automatic(
        "category_parent_picked",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    is SplitBillUiEvent.OnCategoryIconPicked -> EventAnalytics.Automatic(
        "category_icon_picked",
        mapOf("icon" to event.icon, "source" to SPLIT_BILL_SOURCE),
    )

    is SplitBillUiEvent.OnCategoryColorPicked -> EventAnalytics.Automatic(
        "category_color_picked",
        mapOf("color" to event.color.analyticsCode, "source" to SPLIT_BILL_SOURCE),
    )

    is SplitBillUiEvent.OnCustomColor -> customColorAnalyticsOf(event.event)

    // Why: the tap, then the category once it is saved.
    SplitBillUiEvent.OnCategoryFormSaveClicked -> EventAnalytics.Manual(
        setOf("category_save_clicked", "category_saved"),
    )

    // Why: only when its question opens, an answer only while it is open and not deleting yet.
    SplitBillUiEvent.OnCategoryDeleteClicked -> EventAnalytics.Manual("category_delete_clicked")

    SplitBillUiEvent.OnCategoryDeleteConfirmed -> EventAnalytics.Manual("category_delete_confirmed")

    SplitBillUiEvent.OnCategoryDeleteDismissed -> EventAnalytics.Manual("category_delete_cancelled")

    // Why: only while it can read one (online, not editing), as the source choice opens only then.
    SplitBillUiEvent.OnScanReceiptClicked -> EventAnalytics.Manual("scan_receipt_clicked")

    // Why: only a source picked on the choice still open, with which one.
    is SplitBillUiEvent.OnReceiptSourcePicked -> EventAnalytics.Manual("receipt_source_picked")

    // Why: no pick, only the device's lack of a camera; the tap was "Ler cupom fiscal", counted on its own.
    is SplitBillUiEvent.OnReceiptChoiceSkipped -> EventAnalytics.NotTracked

    // Why: a photo handed back, which stands for the tap that opened its picker, then how the read ended.
    is SplitBillUiEvent.OnReceiptPhotoPicked -> EventAnalytics.Manual(setOf("photo_picked", "receipt_read"))

    SplitBillUiEvent.OnReceiptRetryClicked -> EventAnalytics.Manual(setOf("retry_clicked", "receipt_read"))

    SplitBillUiEvent.OnReceiptRemoved -> EventAnalytics.Automatic("receipt_removed")

    // Why: a toggle, sent with where it ended up.
    is SplitBillUiEvent.OnReceiptTorchClicked -> EventAnalytics.Automatic(
        "receipt_camera_torch_toggled",
        mapOf("is_on" to event.isOn),
    )

    SplitBillUiEvent.OnReceiptCameraAllowClicked -> EventAnalytics.Automatic(
        "camera_allow_clicked",
        mapOf("source" to "receipt"),
    )

    SplitBillUiEvent.OnReceiptCameraSettingsClicked -> EventAnalytics.Automatic(
        "camera_settings_clicked",
        mapOf("source" to "receipt"),
    )

    // Why: the tap, then the bill once it is saved.
    SplitBillUiEvent.OnSaveClicked -> EventAnalytics.Manual(setOf("split_bill_save_clicked", "bill_saved"))

    // Why: only when "Apagar esta conta?" opens, an answer only while it is open and not deleting yet.
    SplitBillUiEvent.OnDeleteClicked -> EventAnalytics.Manual("bill_delete_clicked")

    SplitBillUiEvent.OnDeleteConfirmed -> EventAnalytics.Manual("bill_delete_confirmed")

    SplitBillUiEvent.OnDeleteDismissed -> EventAnalytics.Manual("bill_delete_cancelled")
}

// Why: the person's own colors in the form's "Color", each name written here so the catalog test reads it.
private fun customColorAnalyticsOf(event: CustomColorEvent): EventAnalytics = when (event) {
    /*
     * Why: no action of the person's: the picker closing by itself, or the gradient and fields as they change ("Add"
     * tells what came of them).
     */
    CustomColorEvent.OnPickerClosed,
    is CustomColorEvent.OnChannelChanged,
    is CustomColorEvent.OnHexChanged,
    is CustomColorEvent.OnHsvChanged,
    -> EventAnalytics.NotTracked

    CustomColorEvent.OnDeletionDismissed -> EventAnalytics.Automatic(
        "custom_color_delete_cancelled",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    CustomColorEvent.OnDeleteConfirmed -> EventAnalytics.Automatic(
        "custom_color_delete_confirmed",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    is CustomColorEvent.OnLongPressed -> EventAnalytics.Automatic(
        "custom_color_delete_asked",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    CustomColorEvent.OnDraftDismissed -> EventAnalytics.Automatic(
        "custom_color_cancelled",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    CustomColorEvent.OnSaveClicked -> EventAnalytics.Automatic(
        "custom_color_save_clicked",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )

    is CustomColorEvent.OnAddClicked -> EventAnalytics.Automatic(
        "custom_color_add_clicked",
        mapOf("source" to SPLIT_BILL_SOURCE),
    )
}
