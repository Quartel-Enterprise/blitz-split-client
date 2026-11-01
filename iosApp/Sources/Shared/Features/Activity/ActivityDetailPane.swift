import Shared
import SwiftUI

/// The column beside the list (the Mac's inspector): the item picked, who did it and when, what changed before and
/// after (a table on the Mac) or what happened, why a deleted bill or group no longer opens, the button that opens
/// it, and the other events of its bill. With nothing to show, the empty state; while a new device downloads, a note.
struct ActivityDetailPane: View {
    let model: ActivityModel

    /// The narrowest Mac window content that still fits the inspector beside the list.
    static let minimumScreenWidth: CGFloat = 700

    var body: some View {
        ScrollView {
            if let emptyState = model.state.emptyState {
                ActivityEmptyView(model: model, emptyState: emptyState)
                    .padding(.top, 40)
            } else if let item = model.state.selectedItem {
                ActivityItemDetail(model: model, item: item)
                    .id(item.id)
            } else {
                Text("Item details show up here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 80)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .scrollContentBackground(.hidden)
        .background(Palette.groupedBackground)
    }
}

private struct ActivityItemDetail: View {
    let model: ActivityModel
    let item: ActivityItem

    #if os(macOS)
    private static let faceSize: CGFloat = 44
    #else
    private static let faceSize: CGFloat = 56
    #endif

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            header
            if !item.changes.isEmpty {
                Text("What changed")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                ActivityChangesTable(changes: item.changes)
            } else if item.deletion == nil, let detail = ActivityCopy.detail(item) {
                Text(verbatim: detail)
                    .font(.body)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Palette.card, in: .rect(cornerRadius: 16))
            }
            if let deletion = item.deletion {
                Label {
                    Text(verbatim: ActivityCopy.deleted(deletion))
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "trash")
                }
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }
            #if os(iOS)
            if item.deletion == nil {
                openButton
                    .buttonStyle(.borderedProminent)
                    .buttonBorderShape(.capsule)
                    .controlSize(.large)
            }
            #endif
            let related = model.state.relatedItems
            if !related.isEmpty {
                Text("More in \(ActivityCopy.object(item))")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                VStack(spacing: 0) {
                    ForEach(related, id: \.id) { other in
                        ActivityRelatedRow(model: model, item: other)
                        if other.id != related.last?.id {
                            Divider()
                                .padding(.leading, 56)
                        }
                    }
                }
                .background(Palette.card, in: .rect(cornerRadius: 16))
            }
        }
        .padding(20)
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 14) {
            ActivityFace(item: item, size: Self.faceSize)
            VStack(alignment: .leading, spacing: 4) {
                ActivityCopy.headline(item)
                    .font(.title3)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: ActivityCopy.meta(item, days: model.state.days))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            #if os(macOS)
            Spacer(minLength: 8)
            if item.deletion == nil {
                openButton
                    .buttonStyle(.borderedProminent)
            }
            #endif
        }
    }

    private var openButton: some View {
        Button(ActivityCopy.openLabel(item)) {
            model.send(ActivityUiEventOnOpenTargetClicked(itemId: item.id))
        }
    }
}

/// The fields an edit changed: before, struck through, and after. A table with its header on the Mac, as the bill's
/// history draws it; the rows of the row's "See changes" on iPhone and iPad.
private struct ActivityChangesTable: View {
    let changes: [ActivityChangeRow]

    var body: some View {
        #if os(macOS)
        Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 0) {
            GridRow {
                Text("Field")
                Text("Before")
                Text("After")
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(.secondary)
            .padding(.vertical, 6)
            ForEach(Array(changes.enumerated()), id: \.offset) { index, change in
                GridRow {
                    Text(verbatim: ActivityCopy.field(change.field))
                        .foregroundStyle(.secondary)
                    Text(verbatim: ActivityCopy.value(change.before, field: change.field))
                        .strikethrough()
                        .foregroundStyle(.secondary)
                    Text(verbatim: ActivityCopy.value(change.after, field: change.field))
                        .fontWeight(.semibold)
                }
                .padding(.vertical, 8)
                .background(index.isMultiple(of: 2) ? Palette.card : .clear)
                .accessibilityElement(children: .combine)
            }
        }
        .padding(.horizontal, 10)
        #else
        VStack(alignment: .leading, spacing: 10) {
            ForEach(Array(changes.enumerated()), id: \.offset) { _, change in
                ActivityChangeLine(change: change)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Palette.card, in: .rect(cornerRadius: 16))
        #endif
    }
}

/// Another event of the same bill: tapping it shows it in the column instead.
private struct ActivityRelatedRow: View {
    let model: ActivityModel
    let item: ActivityItem

    var body: some View {
        Button {
            model.send(ActivityUiEventOnItemClicked(itemId: item.id))
        } label: {
            HStack(alignment: .top, spacing: 12) {
                AvatarPhoto(initial: item.actor.initials, color: item.actor.color, photoUrl: nil, size: 32)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    ActivityCopy.headline(item)
                        .font(.subheadline)
                    if let detail = ActivityCopy.detail(item) {
                        Text(verbatim: detail)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                }
                Spacer(minLength: 8)
                Text(verbatim: ActivityCopy.time(item))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .padding(12)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

/// A filter in use, with the × that removes it: the person a contact's history opened Activity on, and on the Mac
/// each filter of the bar under the toolbar.
struct ActivityRemovableChip: View {
    let title: String
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(verbatim: title)
                .lineLimit(1)
            Button("Remove filter \(title)", systemImage: "xmark", action: onRemove)
                .labelStyle(.iconOnly)
                .buttonStyle(.borderless)
                .font(.caption.weight(.bold))
        }
        .font(.subheadline.weight(.semibold))
        .foregroundStyle(Palette.onAccent)
        .padding(.leading, 12)
        .padding(.trailing, 8)
        .frame(minHeight: 30)
        .background(Palette.accent, in: .capsule)
        .tint(Palette.onAccent)
    }
}

#if os(iOS)
/// "Group" and "Type", filled with what they filter by once picked, which open Filter; the person a contact's history
/// opened Activity on; and "Clear" while any is in use.
struct ActivityFilterChips: View {
    let model: ActivityModel

    private var state: ActivityUiState {
        model.state
    }

    var body: some View {
        HStack(spacing: 8) {
            let groupName = state.groupChipName ?? String(localized: "Group")
            chip(title: groupName, isOn: state.filter.groupId != nil)
                .accessibilityLabel(Text("Filter by group: \(state.groupChipName ?? String(localized: "All groups"))"))
            chip(title: ActivityCopy.typeChip(state.typeChips), isOn: !state.typeChips.isEmpty)
                .accessibilityLabel(Text("Filter by type: \(ActivityCopy.typeChip(state.typeChips))"))
            if let person = state.personName {
                ActivityRemovableChip(title: person) {
                    model.send(ActivityUiEventOnPersonChipRemoved.shared)
                }
            }
            Spacer(minLength: 0)
            if state.filter.isActive {
                Button("Clear") {
                    model.send(ActivityUiEventOnClearFiltersClicked.shared)
                }
                .buttonStyle(.borderless)
            }
        }
    }

    private func chip(title: String, isOn: Bool) -> some View {
        Button {
            model.send(ActivityUiEventOnFilterClicked.shared)
        } label: {
            HStack(spacing: 6) {
                Text(verbatim: title)
                    .lineLimit(1)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption2.weight(.semibold))
            }
            .font(.subheadline.weight(isOn ? .semibold : .regular))
            .foregroundStyle(isOn ? Palette.onAccent : Color.primary)
            .padding(.horizontal, 12)
            .frame(minHeight: 34)
            .frame(maxWidth: 170)
            .background(isOn ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.fill.tertiary), in: .capsule)
        }
        .buttonStyle(.plain)
    }
}

/// Filter: one group or all, and any of the types (none shows them all), with how many items that shows. ✓ applies
/// it, × and a swipe down keep the filters as they were, and "Clear filters" shows everything. A sheet on iPhone, its
/// two lists side by side lying down, the form sheet's 540 pt on iPad.
struct ActivityFilterSheet: View {
    let model: ActivityModel

    @State private var layout = ScreenLayout()

    private var draft: ActivityFilter {
        model.state.filterDraft ?? model.state.filter
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                Group {
                    if layout.usesTwoColumns {
                        TwoColumnsLayout(spacing: 20) {
                            groupSection
                        } second: {
                            VStack(spacing: 6) {
                                typeSection
                                footer
                            }
                        }
                    } else {
                        VStack(spacing: 6) {
                            groupSection
                            typeSection
                            footer
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
            }
            .background(Palette.groupedBackground)
            .measuringScreenLayout($layout)
            .navigationTitle("Filter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .cancel) {
                        model.send(ActivityUiEventOnFilterDismissed.shared)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(role: .confirm) {
                        model.send(ActivityUiEventOnFilterApplied.shared)
                    } label: {
                        Label {
                            Text(verbatim: String(localized: "Show \(ActivityCopy.results(model.state.draftResultCount))"))
                        } icon: {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        }
        .formSheetSizing()
    }

    private var groupSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionCaption(text: "Group")
                .padding(.vertical, 8)
            GroupedCard {
                VStack(spacing: 0) {
                    groupRow(id: nil, name: String(localized: "All groups"), color: nil)
                    ForEach(model.state.groups, id: \.id) { group in
                        Divider().padding(.leading, 48)
                        groupRow(id: group.id, name: group.name, color: group.color)
                    }
                }
            }
        }
    }

    private var typeSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionCaption(text: "Type")
                .padding(.vertical, 8)
            GroupedCard {
                VStack(spacing: 0) {
                    ForEach(ActivityCopy.types, id: \.self) { type in
                        if type != ActivityCopy.types.first {
                            Divider().padding(.leading, 48)
                        }
                        typeRow(type)
                    }
                }
            }
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("\(ActivityCopy.results(model.state.draftResultCount)) with these filters. No type checked shows them all.")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)
                .fixedSize(horizontal: false, vertical: true)
            Button("Clear filters") {
                model.send(ActivityUiEventOnFilterCleared.shared)
            }
            .buttonStyle(.borderless)
            .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 6)
    }

    private func groupRow(id: String?, name: String, color: AvatarColor?) -> some View {
        let isOn = draft.groupId == id
        return Button {
            model.send(ActivityUiEventOnDraftGroupPicked(groupId: id))
        } label: {
            pickRow(isOn: isOn) {
                RoundedRectangle(cornerRadius: 6)
                    .fill(color?.container ?? .clear)
                    .frame(width: 20, height: 20)
                Text(verbatim: name)
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    private func typeRow(_ type: ActivityType) -> some View {
        let isOn = draft.types.contains(type)
        return Button {
            model.send(ActivityUiEventOnDraftTypeToggled(type: type))
        } label: {
            pickRow(isOn: isOn) {
                Image(systemName: ActivityCopy.typeIcon(type))
                    .foregroundStyle(Palette.accent)
                    .frame(width: 20)
                Text(verbatim: ActivityCopy.type(type))
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? [.isToggle, .isSelected] : .isToggle)
    }

    private func pickRow(isOn: Bool, @ViewBuilder content: () -> some View) -> some View {
        HStack(spacing: 12) {
            content()
            Spacer(minLength: 8)
            Image(systemName: "checkmark")
                .fontWeight(.semibold)
                .foregroundStyle(Palette.accent)
                .opacity(isOn ? 1 : 0)
                .accessibilityHidden(true)
        }
        .padding(.leading, 16)
        .padding(.trailing, 16)
        .frame(minHeight: 44)
        .contentShape(.rect)
    }
}
#endif

#if os(macOS)
/// "Filtered by" and each filter in use, removable, under the toolbar, with "Clear".
struct ActivityFilterBar: View {
    let model: ActivityModel

    var body: some View {
        let state = model.state
        ScrollView(.horizontal) {
            HStack(spacing: 6) {
                Text("Filtered by")
                    .foregroundStyle(.secondary)
                if let group = state.groupChipName {
                    ActivityRemovableChip(title: group) {
                        model.send(ActivityUiEventOnGroupChipRemoved.shared)
                    }
                }
                ForEach(state.typeChips, id: \.self) { type in
                    ActivityRemovableChip(title: ActivityCopy.type(type)) {
                        model.send(ActivityUiEventOnTypeChipRemoved(type: type))
                    }
                }
                if let person = state.personName {
                    ActivityRemovableChip(title: person) {
                        model.send(ActivityUiEventOnPersonChipRemoved.shared)
                    }
                }
                Button("Clear") {
                    model.send(ActivityUiEventOnClearFiltersClicked.shared)
                }
                .buttonStyle(.link)
            }
        }
        .scrollIndicators(.hidden)
        .font(.callout)
    }
}

/// "Mark all as read", spelled out in a wide window and an icon in a narrow one, and Filter, whose popover hangs
/// from it.
struct ActivityMacToolbar: ToolbarContent {
    let model: ActivityModel
    let showsTitles: Bool

    var body: some ToolbarContent {
        ToolbarItem(placement: .primaryAction) {
            Button {
                model.send(ActivityUiEventOnMarkAllReadClicked.shared)
            } label: {
                // A toolbar draws a Label as its icon only: the canvas spells it out in a wide window
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle")
                    if showsTitles {
                        Text("Mark all as read")
                    }
                }
            }
            .disabled(!model.state.canMarkAllRead)
            .help(Text("Mark all as read"))
            .accessibilityLabel(Text("Mark all as read"))
        }
        ToolbarItem(placement: .primaryAction) {
            Button {
                model.send(ActivityUiEventOnFilterClicked.shared)
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                        .symbolVariant(model.state.filter.isActive ? .fill : .none)
                    Text("Filter")
                }
            }
            .popover(isPresented: popoverShown, arrowEdge: .bottom) {
                ActivityFilterPopover(model: model)
            }
        }
    }

    /// Closing the popover applies what was picked in it.
    private var popoverShown: Binding<Bool> {
        Binding(
            get: { model.state.isFilterOpen },
            set: { isOpen in
                if !isOpen, model.current.isFilterOpen { model.send(ActivityUiEventOnFilterApplied.shared) }
            },
        )
    }
}

/// Filter activity: one group or all, any of the types, how many items that shows and "Clear".
private struct ActivityFilterPopover: View {
    let model: ActivityModel

    private var draft: ActivityFilter {
        model.state.filterDraft ?? model.state.filter
    }

    private var groupBinding: Binding<String?> {
        Binding(
            get: { draft.groupId },
            set: { model.send(ActivityUiEventOnDraftGroupPicked(groupId: $0)) },
        )
    }

    private func typeBinding(_ type: ActivityType) -> Binding<Bool> {
        Binding(
            get: { draft.types.contains(type) },
            set: { _ in model.send(ActivityUiEventOnDraftTypeToggled(type: type)) },
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Filter activity")
                .font(.headline)
            Text("Group")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            Picker("Group", selection: groupBinding) {
                Text("All groups").tag(String?.none)
                ForEach(model.state.groups, id: \.id) { group in
                    Text(verbatim: group.name).tag(Optional(group.id))
                }
            }
            .pickerStyle(.radioGroup)
            .labelsHidden()
            Text("Type")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            ForEach(ActivityCopy.types, id: \.self) { type in
                Toggle(isOn: typeBinding(type)) {
                    Label(ActivityCopy.type(type), systemImage: ActivityCopy.typeIcon(type))
                }
                .toggleStyle(.checkbox)
            }
            Divider()
            HStack {
                Text(verbatim: ActivityCopy.results(model.state.draftResultCount))
                    .foregroundStyle(.secondary)
                Spacer()
                Button("Clear") {
                    model.send(ActivityUiEventOnFilterCleared.shared)
                }
            }
        }
        .padding(16)
        .frame(width: 280)
    }
}

/// The words only the Mac's window shows.
enum ActivityMacCopy {
    /// Under the title: "Syncing…", "3 new", "No activity" or "All read".
    static func subtitle(_ state: ActivityUiState) -> String {
        if state.showsPlaceholders { return String(localized: "Syncing…") }
        if state.unreadCount > 0 { return ActivityCopy.unread(state.unreadCount) }
        if state.emptyState == .nothingYet { return String(localized: "No activity") }
        return String(localized: "All read")
    }
}

extension FocusedValues {
    /// The Activity of the window in front, for the Edit menu.
    @Entry var activityModel: ActivityModel?
}

/// Edit › Mark All as Read, as the toolbar's button; disabled with nothing unread or Activity not in front.
struct ActivityCommands: Commands {
    @FocusedValue(\.activityModel) private var model

    var body: some Commands {
        CommandGroup(after: .pasteboard) {
            Divider()
            Button("Mark All as Read") {
                model?.send(ActivityUiEventOnMarkAllReadClicked.shared)
            }
            .disabled(model?.state.canMarkAllRead != true)
        }
    }
}
#endif
