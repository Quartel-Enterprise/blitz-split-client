import Shared
import SwiftUI

/// Bindings of "Item" and of a fee's field: each field reads its filtered text back from `current` in the same
/// keystroke, so a letter typed in an amount never shows for a frame.
extension SplitBillModel {
    var itemName: Binding<String> {
        Binding(
            get: { self.current.itemEditor?.name ?? "" },
            set: { self.send(SplitBillUiEventOnItemNameChanged(text: $0)) },
        )
    }

    var itemPrice: Binding<String> {
        Binding(
            get: { self.current.itemEditor?.unitPriceText ?? "" },
            set: { self.send(SplitBillUiEventOnItemPriceChanged(text: $0)) },
        )
    }

    /// The Mac's "Quantity:" field; the iPhone's stepper sends one more or one less instead.
    var itemQuantity: Binding<String> {
        Binding(
            get: { self.current.itemEditor.map { "\($0.quantity)" } ?? "" },
            set: { self.send(SplitBillUiEventOnItemQuantityChanged(text: $0)) },
        )
    }

    /// A person's field: Amount's and %'s text, or the count of Units and Shares on the Mac.
    func itemValue(of memberId: String) -> Binding<String> {
        Binding(
            get: {
                guard let editor = self.current.itemEditor,
                      let row = editor.rows.first(where: { $0.person.id == memberId }) else { return "" }
                switch editor.mode {
                case .units, .shares: return row.count > 0 ? "\(row.count)" : ""
                default: return row.valueText
                }
            },
            set: { self.send(SplitBillUiEventOnItemPersonValueChanged(memberId: memberId, text: $0)) },
        )
    }

    var feeValue: Binding<String> {
        Binding(
            get: { self.current.feeEditor?.valueText ?? "" },
            set: { self.send(SplitBillUiEventOnFeeValueChanged(text: $0)) },
        )
    }
}

/// Whether it all closes with the total, with 7.5d's two ways out while the items fall short or go over.
private struct ItemsStatusView: View {
    let items: ItemsEditor
    let send: (any SplitBillUiEvent) -> Void

    var body: some View {
        let status = items.status
        let isError = !items.addsUp
        VStack(alignment: .leading, spacing: 8) {
            Text(verbatim: ItemsCopy.statusTitle(status, currency: items.currency))
                .font(.headline)
                .monospacedDigit()
            Text(verbatim: ItemsCopy.statusDetail(status, currency: items.currency))
                .font(.subheadline)
                .fixedSize(horizontal: false, vertical: true)
            if status is ItemsStatusAmountLeft || status is ItemsStatusAmountOver {
                HStack(spacing: 8) {
                    if items.canAddDifferenceItem {
                        Button("Add an item of \(items.currency.format(items.remainingMinor))") {
                            send(SplitBillUiEventOnAddDifferenceItemClicked.shared)
                        }
                    }
                    Button("Split the difference") {
                        send(SplitBillUiEventOnSplitDifferenceClicked.shared)
                    }
                    .disabled(!items.canSplitDifference)
                }
                .buttonStyle(.borderedProminent)
                .tint(Palette.negative)
                #if os(macOS)
                .controlSize(.small)
                #endif
            }
        }
        .foregroundStyle(isError ? AnyShapeStyle(Palette.onErrorContainer) : AnyShapeStyle(.primary))
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

private struct ItemsOfflineNote: View {
    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Circle()
                .fill(.orange)
                .frame(width: 8, height: 8)
                .accessibilityHidden(true)
            #if os(macOS)
            Text("No internet: you can still check everything. The bill stays saved on this Mac and syncs when you reconnect.")
                .fixedSize(horizontal: false, vertical: true)
            #else
            Text("No internet: you can still check everything. The bill stays saved on this iPhone and syncs when you reconnect.")
                .fixedSize(horizontal: false, vertical: true)
            #endif
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
    }
}

/// The first faces of who consumed an item.
private struct ConsumerFaces: View {
    let people: [BillPerson]
    var size: CGFloat = 24

    var body: some View {
        HStack(spacing: -size / 3) {
            ForEach(people.prefix(3), id: \.id) { person in
                AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: size)
                    .overlay(Circle().strokeBorder(Palette.card, lineWidth: 2))
            }
        }
        .accessibilityHidden(true)
    }
}

/// One item of the list: name, how it splits, the faces and its total, in red while it has nobody (7.5c).
private struct ItemRowLabel: View {
    let row: ItemsItemRow
    let currency: GroupCurrency

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: row.item.name)
                    .lineLimit(1)
                Text(verbatim: ItemsCopy.rowSubtitle(row, currency: currency))
                    .font(.footnote)
                    .foregroundStyle(row.isLoose ? AnyShapeStyle(Palette.negative) : AnyShapeStyle(.secondary))
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            ConsumerFaces(people: row.consumers)
            Text(verbatim: currency.format(row.item.totalMinor))
                .monospacedDigit()
                .frame(minWidth: 80, alignment: .trailing)
        }
        .contentShape(.rect)
    }
}

/// "What each one pays": a person, how it adds up, and their total.
private struct ItemsPersonRowView: View {
    let row: ItemsPersonRow
    let currency: GroupCurrency
    var showsDetail = true

    var body: some View {
        HStack(spacing: 12) {
            AvatarPhoto(initial: row.person.initials, color: row.person.avatarColor, photoUrl: nil, size: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: SplitBillCopy.name(row.person))
                if showsDetail {
                    Text(verbatim: ItemsCopy.personSubtitle(row, currency: currency))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            Text(verbatim: currency.format(row.totalMinor))
                .fontWeight(.semibold)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }
}

/// Each one's total as a chip, the name over the amount, in a strip that scrolls sideways.
private struct ItemsPeopleStrip: View {
    let items: ItemsEditor

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(items.personRows, id: \.person.id) { row in
                    HStack(spacing: 8) {
                        AvatarPhoto(initial: row.person.initials, color: row.person.avatarColor, photoUrl: nil, size: 28)
                        VStack(alignment: .leading, spacing: 0) {
                            Text(verbatim: SplitBillCopy.name(row.person))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(verbatim: items.currency.format(row.totalMinor))
                                .font(.subheadline.bold())
                                .monospacedDigit()
                        }
                    }
                    .padding(.leading, 8)
                    .padding(.trailing, 14)
                    .frame(minHeight: 52)
                    .background(Palette.card, in: .rect(cornerRadius: 16))
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }
}

/// "Service (10%)" and "Cover", each opening its field, and the difference "Split the difference" shared.
private struct FeeRows: View {
    let items: ItemsEditor
    let send: (any SplitBillUiEvent) -> Void

    var body: some View {
        Button {
            send(SplitBillUiEventOnFeeClicked(fee: .service))
        } label: {
            feeLabel(ItemsCopy.serviceTitle(items), String(localized: "In proportion to what each one had"), items.breakdown.serviceMinor)
        }
        .buttonStyle(.plain)
        Button {
            send(SplitBillUiEventOnFeeClicked(fee: .couvert))
        } label: {
            feeLabel(String(localized: "Cover"), ItemsCopy.couvertSubtitle(items), items.breakdown.couvertMinor)
        }
        .buttonStyle(.plain)
        if items.showsExtra {
            HStack {
                feeLabel(String(localized: "Difference"), String(localized: "In proportion to what each one had"), items.split.extraMinor, signed: true)
                Button("Remove the difference", systemImage: "xmark.circle.fill") {
                    send(SplitBillUiEventOnDifferenceRemoved.shared)
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.borderless)
                .foregroundStyle(.secondary)
            }
        }
    }

    private func feeLabel(_ title: String, _ subtitle: String, _ minor: Int64, signed: Bool = false) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: title)
                Text(verbatim: subtitle)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Text(verbatim: signed ? items.currency.formatSigned(minor) : items.currency.format(minor))
                .monospacedDigit()
        }
        .contentShape(.rect)
    }
}

/// "Item"'s status: the main line, the bar by Units, Amount and %, and the line under it.
private struct ItemStatusView: View {
    let editor: ItemEditor

    var body: some View {
        let text = ItemsCopy.itemStatus(editor)
        VStack(alignment: .leading, spacing: 8) {
            Text(verbatim: text.title)
                .font(.headline)
                .monospacedDigit()
            if let progress = editor.progress {
                ProgressView(value: Double(progress.floatValue))
                    .tint(editor.isStatusError ? Palette.negative : Palette.accent)
                    .accessibilityHidden(true)
            }
            Text(verbatim: text.detail)
                .font(.subheadline)
                .foregroundStyle(editor.isStatusError ? AnyShapeStyle(Palette.onErrorContainer) : AnyShapeStyle(.secondary))
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(editor.isStatusError ? AnyShapeStyle(Palette.onErrorContainer) : AnyShapeStyle(.primary))
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

private struct ItemModePicker: View {
    let editor: ItemEditor
    let send: (any SplitBillUiEvent) -> Void

    var body: some View {
        Picker("How to split the item", selection: selection) {
            ForEach(editor.modes, id: \.self) { mode in
                ItemsCopy.modeLabel(mode).tag(mode)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
    }

    private var selection: Binding<ItemSplitMode> {
        Binding(
            get: { editor.mode },
            set: { send(SplitBillUiEventOnItemModeClicked(mode: $0)) },
        )
    }
}

/// One person of the item: Equal's checkbox, the iPhone's stepper of Units and Shares (a field on the Mac), or the
/// field of Amount and %.
private struct ItemPersonRowView: View {
    let line: ItemPersonLine
    let editor: ItemEditor
    let model: SplitBillModel

    private var person: BillPerson {
        line.row.person
    }

    private var isIn: Bool {
        line.shareMinor != nil || line.row.isIncluded || line.row.count > 0 || !line.row.valueText.isEmpty
    }

    var body: some View {
        if editor.mode == .equal {
            Button {
                model.send(SplitBillUiEventOnItemPersonToggled(memberId: person.id))
            } label: {
                content
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(line.row.isIncluded ? .isSelected : [])
            .accessibilityLabel(Text("\(person.name) had it"))
        } else {
            content
        }
    }

    private var content: some View {
        HStack(spacing: 12) {
            if editor.mode == .equal {
                Image(systemName: line.row.isIncluded ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(
                        line.row.isIncluded
                            ? AnyShapeStyle(Palette.accent)
                            : editor.isStatusError ? AnyShapeStyle(Palette.negative) : AnyShapeStyle(.tertiary),
                    )
                    .accessibilityHidden(true)
            }
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 32)
                .opacity(isIn ? 1 : 0.5)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: SplitBillCopy.name(person))
                    .foregroundStyle(isIn || line.isError ? .primary : .secondary)
                Text(verbatim: line.shareMinor.map { editor.currency.format($0.int64Value) } ?? "—")
                    .font(.footnote)
                    .foregroundStyle(line.isError ? AnyShapeStyle(Palette.negative) : AnyShapeStyle(.secondary))
                    .monospacedDigit()
            }
            Spacer(minLength: 8)
            trailing
        }
        .contentShape(.rect)
    }

    @ViewBuilder
    private var trailing: some View {
        switch editor.mode {
        case .amount:
            HStack(spacing: 4) {
                Text(verbatim: editor.currency.symbol)
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                field(prompt: editor.currency.zeroPlaceholder, width: 96, label: Text("Amount of \(person.name)"))
            }
        case .percent:
            HStack(spacing: 4) {
                field(prompt: "0", width: 52, label: Text("Percentage of \(person.name)"))
                Text(verbatim: "%")
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            }
        case .units, .shares:
            #if os(macOS)
            field(prompt: "0", width: 52, label: countLabel)
            #else
            HStack(spacing: 8) {
                Text(verbatim: "\(line.row.count)")
                    .monospacedDigit()
                    .foregroundStyle(line.isError ? AnyShapeStyle(Palette.negative) : AnyShapeStyle(.primary))
                    .frame(minWidth: 24, alignment: .trailing)
                Stepper {
                    countLabel
                } onIncrement: {
                    model.send(SplitBillUiEventOnItemPersonIncreased(memberId: person.id))
                } onDecrement: {
                    model.send(SplitBillUiEventOnItemPersonDecreased(memberId: person.id))
                }
                .labelsHidden()
            }
            #endif
        default:
            EmptyView()
        }
    }

    private var countLabel: Text {
        editor.mode == .units ? Text("Units of \(person.name)") : Text("Shares of \(person.name)")
    }

    private func field(prompt: String, width: CGFloat, label: Text) -> some View {
        TextField(text: model.itemValue(of: person.id), prompt: Text(verbatim: prompt)) {
            label
        }
        #if os(iOS)
        .keyboardType(editor.mode == .amount ? .decimalPad : .numberPad)
        #endif
        .inlineValueFieldStyle(width: width, isError: line.isError)
    }
}

/// The service's field and "Delete this item?", over "Bill items" and "Item".
private struct ItemsAlerts: ViewModifier {
    let model: SplitBillModel

    func body(content: Content) -> some View {
        content
            .alert(
                Text(model.state.feeEditor.map { ItemsCopy.feeTitle($0.fee) } ?? "Service"),
                isPresented: feeShown,
            ) {
                TextField(text: model.feeValue, prompt: Text(verbatim: "0")) {
                    Text(verbatim: model.state.feeEditor.map { ItemsCopy.feeField($0.fee) } ?? "")
                }
                #if os(iOS)
                .keyboardType(model.state.feeEditor?.fee == .service ? .numberPad : .decimalPad)
                #endif
                Button("Done") {
                    model.send(SplitBillUiEventOnFeeDone.shared)
                }
                .disabled(model.state.feeEditor?.canSave != true)
                Button("Cancel", role: .cancel) {
                    model.send(SplitBillUiEventOnFeeDismissed.shared)
                }
            } message: {
                Text(verbatim: model.state.feeEditor.map { ItemsCopy.feeHelp($0.fee) } ?? "")
            }
            .alert(
                "Delete this item?",
                isPresented: deletionShown,
                presenting: model.state.itemDeletion,
            ) { _ in
                Button(role: .destructive) {
                    model.send(SplitBillUiEventOnItemDeleteConfirmed.shared)
                } label: {
                    Text(ItemsCopy.deleteConfirm)
                }
                Button("Cancel", role: .cancel) {
                    model.send(SplitBillUiEventOnItemDeleteDismissed.shared)
                }
            } message: { deletion in
                Text(verbatim: ItemsCopy.deletion(deletion, currency: model.state.currency))
            }
    }

    private var feeShown: Binding<Bool> {
        Binding(
            get: { model.state.feeEditor != nil },
            set: { shown in
                if !shown, model.current.feeEditor != nil { model.send(SplitBillUiEventOnFeeDismissed.shared) }
            },
        )
    }

    private var deletionShown: Binding<Bool> {
        Binding(
            get: { model.state.itemDeletion != nil },
            set: { shown in
                if !shown, model.current.itemDeletion != nil { model.send(SplitBillUiEventOnItemDeleteDismissed.shared) }
            },
        )
    }
}

extension View {
    fileprivate func itemsAlerts(_ model: SplitBillModel) -> some View {
        modifier(ItemsAlerts(model: model))
    }
}

#if os(iOS)
/// "Bill items" (7.5), pushed in Dividir's place inside the bill's sheet. Lying down and on iPad lying down: what
/// closes and what each one pays on the left, the items and fees on the right; a phone, lying down and the Duo show
/// each one's total as a strip of chips, an iPad as a list.
struct PhoneItemsEditor: View {
    let model: SplitBillModel

    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if let items = model.state.itemsEditor ?? model.shownItems {
                if layout.usesTwoColumns {
                    HStack(alignment: .top, spacing: 0) {
                        Form { overview(items) }
                        Form { lists(items) }
                    }
                } else {
                    Form {
                        overview(items)
                        lists(items)
                    }
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .measuringScreenLayout($layout)
        .navigationTitle(Text("Bill items"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done", systemImage: "checkmark") {
                    model.send(SplitBillUiEventOnItemsDone.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(model.state.itemsEditor?.addsUp != true)
            }
        }
        .navigationDestination(isPresented: itemShown) {
            PhoneItemEditor(model: model)
        }
        .itemsAlerts(model)
    }

    @ViewBuilder
    private func overview(_ items: ItemsEditor) -> some View {
        Section {
            HStack(spacing: 12) {
                Image(systemName: "fork.knife")
                    .font(.title3)
                    .foregroundStyle(Palette.accent)
                    .frame(width: 44, height: 44)
                    .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 13))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 0) {
                    Text(verbatim: SplitBillCopy.paidBy(model.state.description_, state: model.state))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Text(verbatim: items.currency.format(items.totalMinor))
                        .font(.largeTitle.bold())
                        .monospacedDigit()
                }
            }
            .accessibilityElement(children: .combine)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 0, trailing: 4))
        Section {
            ItemsStatusView(items: items) { model.send($0) }
                .listRowBackground(items.addsUp ? Palette.card : Palette.errorContainer)
        } footer: {
            if model.state.showsOfflineNotice { ItemsOfflineNote() }
        }
        if layout.isLarge {
            Section("What each one pays") {
                ForEach(items.personRows, id: \.person.id) { row in
                    ItemsPersonRowView(row: row, currency: items.currency)
                }
            }
        } else {
            Section("What each one pays") {
                ItemsPeopleStrip(items: items)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }
        }
    }

    @ViewBuilder
    private func lists(_ items: ItemsEditor) -> some View {
        Section {
            ForEach(items.rows, id: \.item.id) { row in
                Button {
                    model.send(SplitBillUiEventOnItemClicked(itemId: row.item.id))
                } label: {
                    HStack {
                        ItemRowLabel(row: row, currency: items.currency)
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                            .accessibilityHidden(true)
                    }
                }
                .buttonStyle(.plain)
                .listRowBackground(row.isLoose ? Palette.errorContainer : Palette.card)
            }
        } header: {
            HStack {
                Text("Items · \(items.rows.count)")
                Spacer()
                Button("Add item", systemImage: "plus") {
                    model.send(SplitBillUiEventOnAddItemClicked.shared)
                }
                .font(.footnote.weight(.semibold))
                .textCase(nil)
                .buttonStyle(.borderless)
                .disabled(!items.canAddItem)
            }
        }
        Section("Service and cover") {
            FeeRows(items: items) { model.send($0) }
        }
    }

    /// Back on "Item" (the button, a swipe) leaves the items as they were.
    private var itemShown: Binding<Bool> {
        Binding(
            get: { model.state.itemEditor != nil },
            set: { shown in
                if !shown, model.current.itemEditor != nil { model.send(SplitBillUiEventOnItemDismissed.shared) }
            },
        )
    }
}

/// "Item" (7.6), pushed over "Bill items": name, quantity, price, the item's total, its modes and whether it closes
/// on the left lying down, who consumed it on the right.
struct PhoneItemEditor: View {
    let model: SplitBillModel

    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if let editor = model.state.itemEditor ?? model.shownItem {
                if layout.usesTwoColumns {
                    HStack(alignment: .top, spacing: 0) {
                        Form { fields(editor) }
                        Form { people(editor) }
                    }
                } else {
                    Form {
                        fields(editor)
                        people(editor)
                    }
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .measuringScreenLayout($layout)
        .navigationTitle(Text("Item"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let itemId = model.state.itemEditor?.itemId {
                ToolbarItem(placement: .primaryAction) {
                    Button("Delete item", systemImage: "trash", role: .destructive) {
                        model.send(SplitBillUiEventOnItemDeleteClicked(itemId: itemId))
                    }
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Done", systemImage: "checkmark") {
                    model.send(SplitBillUiEventOnItemDone.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(model.state.itemEditor?.canSave != true)
            }
        }
    }

    @ViewBuilder
    private func fields(_ editor: ItemEditor) -> some View {
        Section {
            TextField("Item", text: model.itemName)
                .textInputAutocapitalization(.sentences)
            HStack {
                Text("Quantity")
                Spacer()
                Text(verbatim: "\(editor.quantity)")
                    .monospacedDigit()
                Stepper("Quantity") {
                    model.send(SplitBillUiEventOnItemQuantityIncreased.shared)
                } onDecrement: {
                    model.send(SplitBillUiEventOnItemQuantityDecreased.shared)
                }
                .labelsHidden()
            }
            HStack {
                Text("Price of each")
                Spacer()
                Text(verbatim: editor.currency.symbol)
                    .foregroundStyle(.secondary)
                TextField(text: model.itemPrice, prompt: Text(verbatim: editor.currency.zeroPlaceholder)) {
                    Text("Price of each")
                }
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .monospacedDigit()
                .frame(maxWidth: 120)
            }
            HStack {
                Text("Item total")
                    .foregroundStyle(.secondary)
                Spacer()
                Text(verbatim: editor.currency.format(editor.totalMinor))
                    .fontWeight(.semibold)
                    .monospacedDigit()
            }
        }
        Section {
            ItemModePicker(editor: editor) { model.send($0) }
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets())
        Section {
            ItemStatusView(editor: editor)
                .listRowBackground(editor.isStatusError ? Palette.errorContainer : Palette.card)
        }
    }

    private func people(_ editor: ItemEditor) -> some View {
        Section {
            ForEach(editor.lines, id: \.row.person.id) { line in
                ItemPersonRowView(line: line, editor: editor, model: model)
            }
        } header: {
            Text(ItemsCopy.listLabel(editor.mode))
        } footer: {
            Text(verbatim: ItemsCopy.hint(editor.mode, currency: editor.currency))
        }
    }
}
#endif

#if os(macOS)
/// "Bill Items": the sheet's next page after "Split", 860 pt wide (700 in a narrow window), the items in a zebra table
/// on the left with + and − under it, what each one pays on the right, Back and Done at the bottom.
struct MacItemsEditor: View {
    let model: SplitBillModel

    @State private var selection: String?

    var body: some View {
        if let items = model.state.itemsEditor ?? model.shownItems {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Bill Items")
                        .font(.title2.bold())
                    Text(verbatim: SplitBillCopy.paidBy(
                        [model.state.description_.trimmingCharacters(in: .whitespacesAndNewlines), items.currency.format(items.totalMinor)]
                            .filter { !$0.isEmpty }
                            .joined(separator: " · "),
                        state: model.state,
                    ))
                    .foregroundStyle(.secondary)
                }
                ItemsStatusView(items: items) { model.send($0) }
                    .padding(12)
                    .background(
                        items.addsUp ? AnyShapeStyle(.fill.quaternary) : AnyShapeStyle(Palette.errorContainer),
                        in: .rect(cornerRadius: 10),
                    )
                if model.state.showsOfflineNotice { ItemsOfflineNote() }
                HStack(alignment: .top, spacing: 20) {
                    itemsTable(items)
                        .frame(maxWidth: .infinity)
                    VStack(alignment: .leading, spacing: 6) {
                        Text("What Each One Pays")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        ScrollView {
                            VStack(spacing: 0) {
                                ForEach(Array(items.personRows.enumerated()), id: \.element.person.id) { index, row in
                                    if index > 0 { Divider() }
                                    ItemsPersonRowView(row: row, currency: items.currency)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                }
                            }
                        }
                        .frame(maxHeight: 320)
                        .fixedSize(horizontal: false, vertical: true)
                        .background(.background, in: .rect(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).strokeBorder(.separator))
                    }
                    .frame(width: 300)
                }
                HStack(spacing: 8) {
                    Spacer()
                    Button("Back") {
                        model.send(SplitBillUiEventOnItemsDismissed.shared)
                    }
                    .keyboardShortcut(.cancelAction)
                    Button("Done") {
                        model.send(SplitBillUiEventOnItemsDone.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(!items.addsUp)
                }
                .controlSize(.large)
            }
            .padding(20)
            .sheet(isPresented: itemShown) {
                MacItemEditor(model: model)
            }
            .itemsAlerts(model)
        }
    }

    private func itemsTable(_ items: ItemsEditor) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Items")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            List(selection: $selection) {
                ForEach(items.rows, id: \.item.id) { row in
                    ItemRowLabel(row: row, currency: items.currency)
                        .listRowBackground(row.isLoose ? Palette.errorContainer : nil)
                        .tag(row.item.id)
                }
                FeeRows(items: items) { model.send($0) }
                    .selectionDisabled()
            }
            .listStyle(.bordered)
            .alternatingRowBackgrounds()
            .contextMenu(forSelectionType: String.self) { _ in
                EmptyView()
            } primaryAction: { ids in
                if let id = ids.first { model.send(SplitBillUiEventOnItemClicked(itemId: id)) }
            }
            .frame(minHeight: 240, maxHeight: 360)
            HStack(spacing: 0) {
                Button("Add item", systemImage: "plus") {
                    model.send(SplitBillUiEventOnAddItemClicked.shared)
                }
                .disabled(!items.canAddItem)
                Button("Delete item", systemImage: "minus") {
                    if let id = selection { model.send(SplitBillUiEventOnItemDeleteClicked(itemId: id)) }
                }
                .disabled(selection == nil)
            }
            .labelStyle(.iconOnly)
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }

    private var itemShown: Binding<Bool> {
        Binding(
            get: { model.state.itemEditor != nil },
            set: { shown in
                if !shown, model.current.itemEditor != nil { model.send(SplitBillUiEventOnItemDismissed.shared) }
            },
        )
    }
}

/// "Item": a 540 pt sheet over "Bill Items", labels on the right of a form grid, the modes as a segmented control,
/// who consumed it in a bordered list, "Delete Item…" at the bottom left.
struct MacItemEditor: View {
    let model: SplitBillModel

    var body: some View {
        if let editor = model.state.itemEditor ?? model.shownItem {
            VStack(alignment: .leading, spacing: 14) {
                Text("Item")
                    .font(.title2.bold())
                Grid(alignment: .leadingFirstTextBaseline, horizontalSpacing: 8, verticalSpacing: 10) {
                    GridRow {
                        Text("Item:").gridColumnAlignment(.trailing)
                        TextField("Item", text: model.itemName)
                            .labelsHidden()
                    }
                    GridRow {
                        Text("Quantity:")
                        HStack(spacing: 8) {
                            TextField("Quantity", text: model.itemQuantity)
                                .labelsHidden()
                                .multilineTextAlignment(.trailing)
                                .frame(width: 60)
                            Text("Price of each:")
                            TextField(text: model.itemPrice, prompt: Text(verbatim: editor.currency.zeroPlaceholder)) {
                                Text("Price of each")
                            }
                            .labelsHidden()
                            .multilineTextAlignment(.trailing)
                            .frame(width: 110)
                        }
                    }
                    GridRow {
                        Text("Total:")
                        Text(verbatim: editor.currency.format(editor.totalMinor))
                            .fontWeight(.semibold)
                            .monospacedDigit()
                    }
                }
                ItemModePicker(editor: editor) { model.send($0) }
                ItemStatusView(editor: editor)
                    .padding(12)
                    .background(
                        editor.isStatusError ? AnyShapeStyle(Palette.errorContainer) : AnyShapeStyle(.fill.quaternary),
                        in: .rect(cornerRadius: 10),
                    )
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(Array(editor.lines.enumerated()), id: \.element.row.person.id) { index, line in
                            if index > 0 { Divider() }
                            ItemPersonRowView(line: line, editor: editor, model: model)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                        }
                    }
                }
                .frame(maxHeight: 280)
                .fixedSize(horizontal: false, vertical: true)
                .background(.background, in: .rect(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(.separator))
                .accessibilityLabel(Text(ItemsCopy.listLabel(editor.mode)))
                Text(verbatim: ItemsCopy.hint(editor.mode, currency: editor.currency))
                    .font(.callout)
                    .foregroundStyle(.secondary)
                HStack(spacing: 8) {
                    if let itemId = editor.itemId {
                        Button("Delete Item…", role: .destructive) {
                            model.send(SplitBillUiEventOnItemDeleteClicked(itemId: itemId))
                        }
                    }
                    Spacer()
                    Button("Back") {
                        model.send(SplitBillUiEventOnItemDismissed.shared)
                    }
                    .keyboardShortcut(.cancelAction)
                    Button("Done") {
                        model.send(SplitBillUiEventOnItemDone.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(!editor.canSave)
                }
                .controlSize(.large)
            }
            .padding(20)
            .frame(width: 540)
            .itemsAlerts(model)
        }
    }
}
#endif
